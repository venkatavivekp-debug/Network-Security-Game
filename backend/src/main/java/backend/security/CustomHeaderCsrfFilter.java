package backend.security;

import backend.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Compensating CSRF control for the cookie-based session model.
 *
 * <p>Spring Security's stateful CSRF token would force the React frontend to
 * round-trip a token on every mutating call; we deliberately keep the API
 * stateless. Instead we require every state-changing API call to carry a
 * custom request header ({@code X-Requested-With: XMLHttpRequest} by default).
 * Browsers will not let cross-origin HTML forms set this header without a
 * preflight, and our CORS policy denies cross-origin preflights from
 * unrecognized origins, so a successful mutating request must come from the
 * configured frontend origin.
 *
 * <p>The check is skipped for unauthenticated bootstrap routes (login,
 * register, logout) so the very first request still works, and for non-API
 * (GET/HEAD/OPTIONS) requests since they should be safe by definition.
 */
@Component
public class CustomHeaderCsrfFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final List<String> EXEMPT_PATH_PREFIXES = List.of(
            "/auth/login", "/auth/register", "/auth/logout", "/login", "/register", "/logout"
    );

    private final ObjectMapper objectMapper;
    private final String requiredHeader;
    private final String requiredHeaderValue;

    public CustomHeaderCsrfFilter(
            ObjectMapper objectMapper,
            @Value("${app.security.csrf.required-header:X-Requested-With}") String requiredHeader,
            @Value("${app.security.csrf.required-value:XMLHttpRequest}") String requiredHeaderValue
    ) {
        this.objectMapper = objectMapper;
        this.requiredHeader = requiredHeader;
        this.requiredHeaderValue = requiredHeaderValue;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase();
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();

        if (SAFE_METHODS.contains(method) || isExempt(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String presented = request.getHeader(requiredHeader);
        if (presented == null || !presented.equalsIgnoreCase(requiredHeaderValue)) {
            writeForbidden(response, path);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExempt(String path) {
        return EXEMPT_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void writeForbidden(HttpServletResponse response, String path) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse();
        body.setTimestamp(LocalDateTime.now());
        body.setStatus(HttpStatus.FORBIDDEN.value());
        body.setError("Forbidden");
        body.setMessage("Request blocked: missing required security header");
        body.setPath(path);
        body.setDetails(Arrays.asList(
                "Mutating API requests must include " + requiredHeader + ": " + requiredHeaderValue
        ));
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
