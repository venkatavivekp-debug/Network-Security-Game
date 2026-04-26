package backend.controller;

import backend.dto.AuthResponse;
import backend.dto.ApiSuccessResponse;
import backend.dto.LoginRequest;
import backend.dto.RegisterRequest;
import backend.adaptive.AdaptiveSecurityService;
import backend.adaptive.RiskAssessment;
import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.model.User;
import backend.security.ConnectionSecurityService;
import backend.service.UserService;
import backend.util.ApiResponseUtil;
import backend.util.RequestContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final AdaptiveSecurityService adaptiveSecurityService;
    private final AuditService auditService;
    private final RequestContextUtil requestContextUtil;
    private final ConnectionSecurityService connectionSecurityService;

    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            AdaptiveSecurityService adaptiveSecurityService,
            AuditService auditService,
            RequestContextUtil requestContextUtil,
            ConnectionSecurityService connectionSecurityService
    ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.adaptiveSecurityService = adaptiveSecurityService;
        this.auditService = auditService;
        this.requestContextUtil = requestContextUtil;
        this.connectionSecurityService = connectionSecurityService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.ok(ApiResponseUtil.success("Registration successful", httpRequest.getRequestURI(), response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> logout(HttpServletRequest httpRequest) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        String username = current == null ? null : current.getName();
        String ip = requestContextUtil.clientIp(httpRequest);
        String ua = requestContextUtil.userAgent(httpRequest);

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        if (username != null && !"anonymousUser".equals(username)) {
            auditService.record(
                    AuditEventType.AUTH_LOGOUT,
                    username,
                    username,
                    ip,
                    ua,
                    null,
                    Map.of("reason", "user_initiated")
            );
        }
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Logout successful",
                httpRequest.getRequestURI(),
                Map.of("loggedOut", true)
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = requestContextUtil.clientIp(httpRequest);
        String ua = requestContextUtil.userAgent(httpRequest);
        String fpHash = adaptiveSecurityService.fingerprint(ip, ua);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userService.getRequiredByUsername(authentication.getName());

            // Evaluate connection consistency BEFORE recording the new fingerprint, so we
            // can flag a session anomaly when the IP/UA/Accept-Language changed since last
            // successful login. Layered signals come from the live request.
            ConnectionSecurityService.Evaluation connection = connectionSecurityService.evaluate(user, httpRequest);

            // Defense against session fixation: invalidate any pre-auth session and put the
            // SecurityContext in a brand-new session.
            HttpSession existing = httpRequest.getSession(false);
            if (existing != null) {
                existing.invalidate();
            }
            HttpSession fresh = httpRequest.getSession(true);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            fresh.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            userService.recordLoginSuccess(user, fpHash);

            RiskAssessment assessment = adaptiveSecurityService.assess(user, ip, ua, 0);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("riskLevel", assessment.getRiskLevel().name());
            details.put("signals", assessment.getSignals());
            details.put("connectionState", connection.state().name());
            details.put("shiftedSignals", connection.shiftedSignals());
            auditService.record(
                    AuditEventType.AUTH_LOGIN_SUCCESS,
                    user.getUsername(),
                    user.getUsername(),
                    ip,
                    ua,
                    assessment.getRiskScore(),
                    details
            );
            auditService.record(
                    AuditEventType.SESSION_REGENERATED,
                    user.getUsername(),
                    user.getUsername(),
                    ip,
                    ua,
                    null,
                    Map.of("reason", "post_login")
            );

            AuthResponse response = new AuthResponse(user.getUsername(), user.getRole(), "Login successful");

            return ResponseEntity.ok(ApiResponseUtil.success("Login successful", httpRequest.getRequestURI(), response));
        } catch (LockedException ex) {
            // Preserve global handler behavior; still emit audit.
            auditService.record(AuditEventType.AUTH_ACCOUNT_LOCKED, request.getUsername(), request.getUsername(), ip, ua, null, Map.of("reason", "locked"));
            throw ex;
        } catch (RuntimeException ex) {
            // Failed auth increments failed attempts when user exists.
            try {
                User user = userService.getRequiredByUsername(request.getUsername());
                userService.recordLoginFailure(user);
                RiskAssessment assessment = adaptiveSecurityService.assess(user, ip, ua, 0);
                auditService.record(AuditEventType.AUTH_LOGIN_FAILURE, request.getUsername(), request.getUsername(), ip, ua, assessment.getRiskScore(), Map.of("signals", assessment.getSignals()));

                if (assessment.getRiskLevel() == backend.adaptive.RiskLevel.CRITICAL) {
                    userService.lockAccount(user, adaptiveSecurityService.lockUntilNowPlusMinutes());
                    auditService.record(AuditEventType.AUTH_ACCOUNT_LOCKED, request.getUsername(), request.getUsername(), ip, ua, assessment.getRiskScore(), Map.of("reason", "critical_risk"));
                }
            } catch (Exception ignored) {
                auditService.record(AuditEventType.AUTH_LOGIN_FAILURE, request.getUsername(), request.getUsername(), ip, ua, null, Map.of("note", "user_not_found_or_audit_failed"));
            }
            throw ex;
        }
    }
}
