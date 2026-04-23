package backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        if (ApiRequestUtil.isApiRequest(request)) {
            ApiRequestUtil.writeError(
                    response,
                    objectMapper,
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required",
                    request.getRequestURI(),
                    List.of("Provide valid credentials to access this endpoint")
            );
            return;
        }

        response.sendRedirect("/login?error=auth");
    }
}
