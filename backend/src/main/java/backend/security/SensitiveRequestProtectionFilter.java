package backend.security;

import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.dto.ApiErrorResponse;
import backend.model.User;
import backend.service.UserService;
import backend.util.RequestContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Replay + integrity protection for a small set of sensitive endpoints:
 * puzzle solve, message decrypt, and admin state-changing actions.
 *
 * <p>Requirements enforced:
 * - {@code X-Req-Nonce}: UUID, must be unique per session within TTL
 * - {@code X-Req-Ts}: epoch millis, must be within TTL window
 * - {@code X-Req-Sig}: HMAC over (method, path, body, ts, nonce)
 *
 * <p>This is a pragmatic edge-hardening layer for an authenticated,
 * session-cookie application. It is intentionally in-memory and per-session.
 */
@Component
public class SensitiveRequestProtectionFilter extends OncePerRequestFilter {

    public static final String HEADER_NONCE = "X-Req-Nonce";
    public static final String HEADER_TS = "X-Req-Ts";
    public static final String HEADER_SIG = "X-Req-Sig";

    private final ObjectMapper objectMapper;
    private final SessionIntegrityService sessionIntegrityService;
    private final ReplayNonceStore replayNonceStore;
    private final UserService userService;
    private final AdaptiveThrottleService throttleService;
    private final AuditService auditService;
    private final RequestContextUtil requestContextUtil;

    private final Duration ttl;

    public SensitiveRequestProtectionFilter(
            ObjectMapper objectMapper,
            SessionIntegrityService sessionIntegrityService,
            ReplayNonceStore replayNonceStore,
            UserService userService,
            AdaptiveThrottleService throttleService,
            AuditService auditService,
            RequestContextUtil requestContextUtil,
            @Value("${app.security.replay.ttl-seconds:120}") long ttlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.sessionIntegrityService = sessionIntegrityService;
        this.replayNonceStore = replayNonceStore;
        this.userService = userService;
        this.throttleService = throttleService;
        this.auditService = auditService;
        this.requestContextUtil = requestContextUtil;
        this.ttl = Duration.ofSeconds(Math.max(30, Math.min(600, ttlSeconds)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method) && !"DELETE".equals(method)) {
            return true;
        }
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        if (path.startsWith("/auth/")) {
            return true;
        }
        boolean puzzleSolve = path.startsWith("/puzzle/solve/") || (path.startsWith("/puzzle/") && path.contains("/solve"));
        return !(puzzleSolve
                || path.startsWith("/message/decrypt/")
                || path.startsWith("/admin/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        replayNonceStore.pruneExpired();

        byte[] bodyBytes = readBodyBytes(request);
        BufferedRequest wrapped = new BufferedRequest(request, bodyBytes);

        HttpSession session = wrapped.getSession(false);
        if (session == null) {
            // If there's no session, security chain will likely return 401 anyway.
            filterChain.doFilter(wrapped, response);
            return;
        }

        String nonce = wrapped.getHeader(HEADER_NONCE);
        String ts = wrapped.getHeader(HEADER_TS);
        String sig = wrapped.getHeader(HEADER_SIG);

        if (!isUuid(nonce) || ts == null || ts.isBlank() || sig == null || sig.isBlank()) {
            write403(wrapped, response, "REQUEST_PROTECTION_REQUIRED",
                    List.of("Provide X-Req-Nonce, X-Req-Ts, X-Req-Sig for sensitive actions"));
            record(AuditEventType.INTEGRITY_FAILED, wrapped, Map.of("reason", "missing_headers"));
            return;
        }

        long tsMs;
        try {
            tsMs = Long.parseLong(ts.trim());
        } catch (NumberFormatException ex) {
            write403(wrapped, response, "REQUEST_PROTECTION_REQUIRED", List.of("Invalid X-Req-Ts"));
            record(AuditEventType.INTEGRITY_FAILED, wrapped, Map.of("reason", "bad_timestamp"));
            return;
        }

        long now = Instant.now().toEpochMilli();
        if (Math.abs(now - tsMs) > ttl.toMillis()) {
            write403(wrapped, response, "REPLAY_BLOCKED", List.of("Expired request timestamp"));
            record(AuditEventType.REPLAY_BLOCKED, wrapped, Map.of("reason", "expired_timestamp"));
            return;
        }

        if (!replayNonceStore.storeIfNew(session.getId(), nonce)) {
            write403(wrapped, response, "REPLAY_BLOCKED", List.of("Nonce already used"));
            record(AuditEventType.REPLAY_BLOCKED, wrapped, Map.of("reason", "nonce_reuse"));
            return;
        }

        String secretB64 = sessionIntegrityService.getOrCreateSecretB64(session);
        String pathWithQuery = withQuery(wrapped);
        String body = bodyBytes == null ? "" : new String(bodyBytes, wrapped.getCharacterEncoding() == null ? java.nio.charset.StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(wrapped.getCharacterEncoding()));

        String canonical = RequestIntegrityService.canonical(wrapped.getMethod(), pathWithQuery, body, String.valueOf(tsMs), nonce);
        if (!RequestIntegrityService.verify(secretB64, canonical, sig)) {
            write403(wrapped, response, "REQUEST_INTEGRITY_FAILED", List.of("Signature invalid"));
            record(AuditEventType.INTEGRITY_FAILED, wrapped, Map.of("reason", "bad_signature"));
            return;
        }

        // Anomaly-driven enforcement: apply small delay for rapid requests / burst failures.
        User user = currentUser();
        if (user != null) {
            long delay = throttleService.computeDelayMs(user, wrapped.getRequestURI());
            if (delay > 0) {
                response.setHeader("X-NSG-Throttle-Ms", String.valueOf(delay));
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                record(AuditEventType.THROTTLE_APPLIED, wrapped, Map.of("delayMs", delay));
            }
        }

        filterChain.doFilter(wrapped, response);
    }

    private byte[] readBodyBytes(HttpServletRequest request) throws IOException {
        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return new byte[0];
        }
        // Read up-front so we can verify the signature without breaking downstream reads.
        return request.getInputStream().readAllBytes();
    }

    private static class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        BufferedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body == null ? new byte[0] : body;
        }

        @Override
        public ServletInputStream getInputStream() {
            byte[] bytes = body;
            return new ServletInputStream() {
                private int idx = 0;

                @Override
                public int read() {
                    if (idx >= bytes.length) return -1;
                    return bytes[idx++] & 0xff;
                }

                @Override
                public boolean isFinished() {
                    return idx >= bytes.length;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // not async
                }
            };
        }
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        try {
            return userService.getRequiredByUsername(auth.getName());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void record(AuditEventType type, HttpServletRequest req, Map<String, Object> extra) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
            String ip = requestContextUtil.clientIp(req);
            String ua = requestContextUtil.userAgent(req);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("path", req.getRequestURI());
            details.put("method", req.getMethod());
            if (extra != null) details.putAll(extra);
            auditService.record(type, username, null, ip, ua, null, details);
        } catch (RuntimeException ignored) {
        }
    }

    private void write403(HttpServletRequest request, HttpServletResponse response, String message, List<String> details) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse();
        body.setTimestamp(LocalDateTime.now());
        body.setSuccess(false);
        body.setStatus(HttpStatus.FORBIDDEN.value());
        body.setError("Forbidden");
        body.setMessage(message);
        body.setPath(request.getRequestURI());
        body.setDetails(details);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private boolean isUuid(String value) {
        if (value == null) return false;
        try {
            UUID.fromString(value.trim());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String withQuery(HttpServletRequest req) {
        String uri = req.getRequestURI() == null ? "" : req.getRequestURI();
        String qs = req.getQueryString();
        if (qs == null || qs.isBlank()) return uri;
        return uri + "?" + qs;
    }
}

