package backend.security;

import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.exception.AdminStepUpRequiredException;
import backend.model.Role;
import backend.model.User;
import backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Lightweight password-based step-up for sensitive admin actions.
 *
 * <p>An admin who is already authenticated can call {@link #confirm} with their
 * password to mint a short-lived token. Sensitive endpoints then call
 * {@link #assertConfirmed} which expects the token in the {@code X-Admin-Confirm}
 * header (or {@code adminConfirmToken} query parameter). The token is bound to
 * the admin's username and expires after {@link #STEP_UP_TTL}.
 *
 * <p>This is not multi-factor; it's a deliberate, short, in-memory friction
 * layer so that misuse of an active admin session can't immediately drive
 * destructive actions like releasing held messages or unlocking users.
 */
@Service
public class AdminStepUpService {

    public static final Duration STEP_UP_TTL = Duration.ofMinutes(5);
    public static final String HEADER_NAME = "X-Admin-Confirm";
    public static final String QUERY_PARAM = "adminConfirmToken";

    public record StepUpToken(String token, LocalDateTime issuedAt, LocalDateTime expiresAt) {

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final ConcurrentMap<String, StepUpToken> tokens = new ConcurrentHashMap<>();

    public AdminStepUpService(UserService userService, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * Verify the admin's password and mint a fresh confirmation token. Wrong
     * password throws {@link AdminStepUpRequiredException} so the caller never
     * leaks whether the user exists.
     */
    public StepUpToken confirm(Authentication authentication, String password, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AdminStepUpRequiredException("Admin authentication required");
        }
        String username = authentication.getName();
        User user;
        try {
            user = userService.getRequiredByUsername(username);
        } catch (RuntimeException ex) {
            throw new AdminStepUpRequiredException("Password did not match");
        }
        if (user.getRole() != Role.ADMIN) {
            throw new AdminStepUpRequiredException("Step-up is reserved for admin accounts");
        }
        if (password == null || password.isBlank() || !passwordEncoder.matches(password, user.getPassword())) {
            try {
                auditService.record(
                        AuditEventType.ADMIN_ACTION,
                        username,
                        username,
                        request == null ? null : request.getRemoteAddr(),
                        request == null ? null : request.getHeader("User-Agent"),
                        null,
                        Map.of("action", "admin_step_up_failed")
                );
            } catch (RuntimeException ignored) {
                // auditing must never crash the auth path
            }
            throw new AdminStepUpRequiredException("Password did not match");
        }

        LocalDateTime now = LocalDateTime.now();
        StepUpToken token = new StepUpToken(UUID.randomUUID().toString(), now, now.plus(STEP_UP_TTL));
        tokens.put(username, token);
        try {
            auditService.record(
                    AuditEventType.ADMIN_ACTION,
                    username,
                    username,
                    request == null ? null : request.getRemoteAddr(),
                    request == null ? null : request.getHeader("User-Agent"),
                    null,
                    Map.of("action", "admin_step_up_confirmed", "ttlSeconds", STEP_UP_TTL.toSeconds())
            );
        } catch (RuntimeException ignored) {
        }
        return token;
    }

    /** Returns the current confirmation status for the admin, or {@code null} when not confirmed. */
    public StepUpToken status(Authentication authentication) {
        if (authentication == null) return null;
        StepUpToken existing = tokens.get(authentication.getName());
        if (existing == null) return null;
        if (existing.isExpired()) {
            tokens.remove(authentication.getName(), existing);
            return null;
        }
        return existing;
    }

    /**
     * Throws {@link AdminStepUpRequiredException} unless the admin has a valid,
     * non-expired confirmation token presented in the request.
     */
    public void assertConfirmed(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AdminStepUpRequiredException("Admin step-up required");
        }
        StepUpToken active = tokens.get(authentication.getName());
        if (active == null || active.isExpired()) {
            tokens.remove(authentication.getName());
            throw new AdminStepUpRequiredException("Admin step-up required");
        }
        String presented = readPresentedToken(request);
        if (presented == null || !presented.equals(active.token())) {
            throw new AdminStepUpRequiredException("Admin step-up token missing or invalid");
        }
    }

    /** Test/admin support: invalidate an admin's confirmation. */
    public void revoke(String username) {
        if (username != null) {
            tokens.remove(username);
        }
    }

    private String readPresentedToken(HttpServletRequest request) {
        if (request == null) return null;
        String header = request.getHeader(HEADER_NAME);
        if (header != null && !header.isBlank()) return header.trim();
        String param = request.getParameter(QUERY_PARAM);
        if (param != null && !param.isBlank()) return param.trim();
        return null;
    }
}
