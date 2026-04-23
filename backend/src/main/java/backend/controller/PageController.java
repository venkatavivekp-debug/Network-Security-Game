package backend.controller;

import backend.dto.AuthResponse;
import backend.dto.LoginRequest;
import backend.dto.MessageDecryptResponse;
import backend.dto.MessageSendRequest;
import backend.dto.RegisterRequest;
import backend.exception.BadRequestException;
import backend.model.AlgorithmType;
import backend.model.Role;
import backend.model.User;
import backend.service.MessageService;
import backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
@Validated
public class PageController {

    private final UserService userService;
    private final MessageService messageService;
    private final AuthenticationManager authenticationManager;
    private final Validator validator;

    public PageController(
            UserService userService,
            MessageService messageService,
            AuthenticationManager authenticationManager,
            Validator validator
    ) {
        this.userService = userService;
        this.messageService = messageService;
        this.authenticationManager = authenticationManager;
        this.validator = validator;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SENDER"))) {
            return "redirect:/send";
        }

        return "redirect:/receive";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null && !model.containsAttribute("notice")) {
            if ("auth".equals(error)) {
                model.addAttribute("notice", "Please login to continue.");
            } else if ("forbidden".equals(error)) {
                model.addAttribute("notice", "You do not have permission to access that resource.");
            }
        }
        return "login";
    }

    @PostMapping("/ui/register")
    public String register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam Role role,
            RedirectAttributes redirectAttributes
    ) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setRole(role);
        validateOrThrow(request);

        AuthResponse response = userService.register(request);
        redirectAttributes.addFlashAttribute("notice", response.getMessage());
        return "redirect:/login";
    }

    @PostMapping("/ui/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        validateOrThrow(loginRequest);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        User user = userService.getRequiredByUsername(authentication.getName());
        redirectAttributes.addFlashAttribute("notice", "Login successful");

        if (user.getRole() == Role.SENDER) {
            return "redirect:/send";
        }

        return "redirect:/receive";
    }

    @GetMapping("/send")
    @PreAuthorize("hasRole('SENDER')")
    public String sendPage(Model model) {
        model.addAttribute("algorithms", AlgorithmType.values());
        return "send";
    }

    @PostMapping("/ui/send")
    @PreAuthorize("hasRole('SENDER')")
    public String sendMessage(
            @RequestParam String receiverUsername,
            @RequestParam String content,
            @RequestParam AlgorithmType algorithmType,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        MessageSendRequest request = new MessageSendRequest();
        request.setReceiverUsername(receiverUsername);
        request.setContent(content);
        request.setAlgorithmType(algorithmType);
        validateOrThrow(request);

        messageService.sendMessage(authentication.getName(), request);
        redirectAttributes.addFlashAttribute("notice", "Message sent successfully");
        return "redirect:/send";
    }

    @GetMapping("/receive")
    @PreAuthorize("hasRole('RECEIVER')")
    public String receivePage(Authentication authentication, Model model) {
        model.addAttribute("messages", messageService.getReceivedMessages(authentication.getName()));
        return "receive";
    }

    @PostMapping("/ui/decrypt/{id}")
    @PreAuthorize("hasRole('RECEIVER')")
    public String decryptMessage(
            @PathVariable("id") @Positive(message = "id must be positive") Long messageId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        MessageDecryptResponse response = messageService.decryptMessage(messageId, authentication.getName());
        redirectAttributes.addFlashAttribute("decryptedMessage", response);
        redirectAttributes.addFlashAttribute("notice", "Message decrypted");
        return "redirect:/receive";
    }

    private <T> void validateOrThrow(T bean) {
        Set<ConstraintViolation<T>> violations = validator.validate(bean);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new BadRequestException(message);
        }
    }
}
