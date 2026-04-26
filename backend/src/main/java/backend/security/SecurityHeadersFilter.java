package backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds a small, opinionated set of OWASP-aligned security headers.
 *
 * <p>The headers default to a strict-but-friendly profile: a CSP that does not
 * permit inline-script or remote script, click-jack protection, MIME sniffing
 * protection, and tight referrer / permissions policies. Sensitive API
 * responses additionally suppress caching so credentials and session-bound
 * data don't get persisted by intermediaries.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final String contentSecurityPolicy;

    public SecurityHeadersFilter(
            @Value("${app.security.csp:default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'}")
            String contentSecurityPolicy
    ) {
        this.contentSecurityPolicy = contentSecurityPolicy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Always-on baseline.
        if (!response.containsHeader("X-Content-Type-Options")) {
            response.setHeader("X-Content-Type-Options", "nosniff");
        }
        if (!response.containsHeader("X-Frame-Options")) {
            response.setHeader("X-Frame-Options", "DENY");
        }
        if (!response.containsHeader("Referrer-Policy")) {
            response.setHeader("Referrer-Policy", "no-referrer");
        }
        if (!response.containsHeader("Permissions-Policy")) {
            response.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=(), payment=(), usb=()");
        }
        if (!response.containsHeader("Content-Security-Policy")) {
            response.setHeader("Content-Security-Policy", contentSecurityPolicy);
        }

        // Sensitive API responses: never cache.
        String path = request.getRequestURI();
        if (path != null && (
                path.startsWith("/auth")
                        || path.startsWith("/admin")
                        || path.startsWith("/message")
                        || path.startsWith("/puzzle")
                        || path.startsWith("/attack")
                        || path.startsWith("/security"))) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        filterChain.doFilter(request, response);
    }
}
