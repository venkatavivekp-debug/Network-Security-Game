package backend.util;

import backend.dto.ApiSuccessResponse;

import java.time.LocalDateTime;

public final class ApiResponseUtil {

    private ApiResponseUtil() {
    }

    public static <T> ApiSuccessResponse<T> success(String message, String path, T data) {
        ApiSuccessResponse<T> response = new ApiSuccessResponse<>();
        response.setTimestamp(LocalDateTime.now());
        response.setSuccess(true);
        response.setMessage(message);
        response.setPath(path);
        response.setData(data);
        return response;
    }
}
