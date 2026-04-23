package backend.security;

import backend.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiRequestUtil {

    private ApiRequestUtil() {
    }

    public static boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/auth/")
                || uri.startsWith("/message/")
                || uri.startsWith("/attack/")
                || "/simulation/run".equals(uri)
                || "/simulation/history".equals(uri)
                || uri.matches("^/simulation/history/\\d+$")
                || "/simulation/compare".equals(uri)
                || "/simulation/advanced-run".equals(uri)
                || "/simulation/advanced-history".equals(uri)
                || uri.matches("^/simulation/advanced-history/\\d+$")
                || "/simulation/evaluate".equals(uri)
                || "/simulation/evaluations".equals(uri)
                || uri.matches("^/simulation/evaluations/\\d+$")
                || "/simulation/evaluate/compare-security".equals(uri)
                || "/simulation/evaluate/compare-defense".equals(uri)
                || "/simulation/evaluate/export".equals(uri)
                || "/simulation/export".equals(uri);
    }

    public static void writeError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            String message,
            String path,
            List<String> details
    ) throws IOException {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setTimestamp(LocalDateTime.now());
        error.setSuccess(false);
        error.setStatus(status.value());
        error.setError(status.getReasonPhrase());
        error.setMessage(message);
        error.setPath(path);
        error.setDetails(details);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
