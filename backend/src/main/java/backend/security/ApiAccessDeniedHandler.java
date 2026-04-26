package backend.security;

import backend.audit.AuditEventType;
import backend.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper, AuditService auditService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        recordForbidden(request, accessDeniedException);
        if (ApiRequestUtil.isApiRequest(request)) {
            ApiRequestUtil.writeError(
                    response,
                    objectMapper,
                    HttpStatus.FORBIDDEN,
                    "Access denied",
                    request.getRequestURI(),
                    List.of("You do not have permission to perform this action")
            );
            return;
        }

        response.sendRedirect("/login?error=forbidden");
    }

    private void recordForbidden(HttpServletRequest request, AccessDeniedException accessDeniedException) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("path", request.getRequestURI());
            details.put("method", request.getMethod());
            String reason = accessDeniedException == null ? null : accessDeniedException.getMessage();
            if (reason != null && !reason.isBlank()) {
                details.put("reason", reason.length() > 120 ? reason.substring(0, 120) : reason);
            }
            auditService.record(
                    AuditEventType.FORBIDDEN_ACCESS,
                    username,
                    null,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"),
                    null,
                    details
            );
        } catch (RuntimeException ignored) {
            // never let auditing break the 403
        }
    }
}
