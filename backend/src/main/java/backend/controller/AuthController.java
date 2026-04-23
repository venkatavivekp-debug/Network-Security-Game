package backend.controller;

import backend.dto.AuthResponse;
import backend.dto.ApiSuccessResponse;
import backend.dto.LoginRequest;
import backend.dto.RegisterRequest;
import backend.model.User;
import backend.service.UserService;
import backend.util.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.ok(ApiResponseUtil.success("Registration successful", httpRequest.getRequestURI(), response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        httpRequest.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        User user = userService.getRequiredByUsername(authentication.getName());
        AuthResponse response = new AuthResponse(user.getUsername(), user.getRole(), "Login successful");

        return ResponseEntity.ok(ApiResponseUtil.success("Login successful", httpRequest.getRequestURI(), response));
    }
}
