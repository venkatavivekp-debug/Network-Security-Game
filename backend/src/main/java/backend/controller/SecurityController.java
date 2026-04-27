package backend.controller;

import backend.dto.ApiSuccessResponse;
import backend.security.SessionIntegrityService;
import backend.util.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Small security bootstrap endpoints for the frontend.
 *
 * <p>The UI fetches the per-session integrity key once after login and uses it
 * to sign sensitive actions (puzzle solve, decrypt, admin actions).
 */
@RestController
@RequestMapping("/security")
public class SecurityController {

    private final SessionIntegrityService sessionIntegrityService;

    public SecurityController(SessionIntegrityService sessionIntegrityService) {
        this.sessionIntegrityService = sessionIntegrityService;
    }

    @GetMapping("/integrity-key")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> integrityKey(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(true);
        String secretB64 = sessionIntegrityService.getOrCreateSecretB64(session);
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Integrity key fetched",
                httpRequest.getRequestURI(),
                Map.of(
                        "secretB64", secretB64
                )
        ));
    }
}

