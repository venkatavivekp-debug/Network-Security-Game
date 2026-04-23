package backend.security.ratelimit;

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
import java.util.List;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final RequestContextUtil requestContextUtil;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiterService rateLimiterService, RequestContextUtil requestContextUtil, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.requestContextUtil = requestContextUtil;
        this.objectMapper = objectMapper;
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
        // - send: prevent flooding
        // - puzzle solve: prevent brute forcing & DoS
        // - admin actions: prevent misuse & automation
        if (path.startsWith("/auth/login")) {
            rateLimiterService.checkOrThrow("login|" + ip, 10, 10.0 / 60.0); // 10/min per IP
        } else if (path.startsWith("/message/send")) {
            rateLimiterService.checkOrThrow("send|" + keyBase, 20, 20.0 / 60.0); // 20/min per user+ip
        } else if (path.startsWith("/puzzle/solve/")) {
            rateLimiterService.checkOrThrow("puzzle|" + keyBase, 12, 12.0 / 60.0); // 12/min per user+ip+msg
        } else if (path.startsWith("/admin/lock-user") || path.startsWith("/admin/unlock-user") || path.startsWith("/admin/hold-message") || path.startsWith("/admin/release-message")) {
            rateLimiterService.checkOrThrow("admin|" + keyBase, 30, 30.0 / 60.0); // 30/min
        }
    }

    private void write429(HttpServletRequest request, HttpServletResponse response, RateLimitExceededException ex) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse();
        body.setTimestamp(LocalDateTime.now());
        body.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        body.setError("Too Many Requests");
        body.setMessage(ex.getMessage());
        body.setPath(request.getRequestURI());
        body.setDetails(List.of("Retry after " + ex.getRetryAfterSeconds() + " seconds"));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}

