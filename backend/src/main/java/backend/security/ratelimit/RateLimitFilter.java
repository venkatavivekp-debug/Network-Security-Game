package backend.security.ratelimit;

import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.dto.ApiErrorResponse;
import backend.util.RequestContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Token-bucket rate limiter applied to a small set of sensitive POST endpoints.
 *
 * <p>The buckets are intentionally generous for a research demo (brute-force
 * protection, not full WAF semantics). When a bucket is exhausted we return a
 * single, clean {@code 429} body with a {@code Retry-After} header and emit a
 * {@link AuditEventType#RATE_LIMIT_BLOCKED} audit event so the SOC view sees it.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final RequestContextUtil requestContextUtil;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public RateLimitFilter(RateLimiterService rateLimiterService, RequestContextUtil requestContextUtil, ObjectMapper objectMapper, AuditService auditService) {
        this.rateLimiterService = rateLimiterService;
        this.requestContextUtil = requestContextUtil;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            enforce(request);
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException ex) {
            write429(request, response, ex);
        }
    }

    private void enforce(HttpServletRequest request) {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String method = request.getMethod() == null ? "" : request.getMethod();

        if (!"POST".equalsIgnoreCase(method)) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
        String ip = requestContextUtil.clientIp(request);
        String keyBase = ip + "|" + username + "|" + path;

        // Bounded, research-appropriate defaults:
        // - login: brute-force protection (IP-based dominates)
        // - register: prevent automated account-creation abuse
        // - send: prevent flooding
        // - puzzle solve: prevent brute forcing & DoS
        // - admin actions: prevent misuse & automation
        if (path.startsWith("/auth/login")) {
            rateLimiterService.checkOrThrow("login|" + ip, 10, 10.0 / 60.0); // 10/min per IP
        } else if (path.startsWith("/auth/register")) {
            rateLimiterService.checkOrThrow("register|" + ip, 5, 5.0 / 60.0); // 5/min per IP
        } else if (path.startsWith("/message/send")) {
            rateLimiterService.checkOrThrow("send|" + keyBase, 20, 20.0 / 60.0); // 20/min per user+ip
        } else if (path.startsWith("/puzzle/solve/")) {
            rateLimiterService.checkOrThrow("puzzle|" + keyBase, 12, 12.0 / 60.0); // 12/min per user+ip+msg
        } else if (path.startsWith("/admin/")) {
            // Catches lock/unlock, hold/release, threat-level, reset-failures, etc.
            rateLimiterService.checkOrThrow("admin|" + keyBase, 30, 30.0 / 60.0); // 30/min
        }
    }

    private void write429(HttpServletRequest request, HttpServletResponse response, RateLimitExceededException ex) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
        String ip = requestContextUtil.clientIp(request);
        String ua = request.getHeader("User-Agent");

        ApiErrorResponse body = new ApiErrorResponse();
        body.setTimestamp(LocalDateTime.now());
        body.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        body.setError("Too Many Requests");
        body.setMessage("Too many requests. Slow down and retry shortly.");
        body.setPath(request.getRequestURI());
        body.setDetails(List.of("retry-after-seconds=" + ex.getRetryAfterSeconds()));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);

        try {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("path", request.getRequestURI());
            details.put("retryAfterSeconds", ex.getRetryAfterSeconds());
            auditService.record(
                    AuditEventType.RATE_LIMIT_BLOCKED,
                    username,
                    null,
                    ip,
                    ua,
                    null,
                    details
            );
        } catch (RuntimeException ignored) {
            // Auditing must never crash a 429.
        }
    }
}

