package backend.exception;

import backend.controller.PageController;
import backend.controller.SimulationPageController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = {PageController.class, SimulationPageController.class})
public class PageExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageExceptionHandler.class);

    @ExceptionHandler({BadRequestException.class, ResourceNotFoundException.class, ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    public String handleKnown(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("notice", userMessage(ex));
        return "redirect:" + fallbackPath(request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("notice", "You do not have permission to access this page.");
        return "redirect:" + fallbackPath(request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public String handleAuthentication(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("notice", "Invalid username or password.");
        return "redirect:/login";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        LOGGER.error("Unhandled page exception", ex);
        redirectAttributes.addFlashAttribute("notice", "Something went wrong. Please try again.");
        return "redirect:" + fallbackPath(request.getRequestURI());
    }

    private String userMessage(Exception ex) {
        if (ex instanceof ConstraintViolationException violationException) {
            return violationException.getConstraintViolations().stream()
                    .findFirst()
                    .map(v -> v.getMessage())
                    .orElse("Validation failed.");
        }
        if (ex instanceof MethodArgumentTypeMismatchException) {
            return "Invalid input value. Please verify the form fields.";
        }
        return ex.getMessage();
    }

    private String fallbackPath(String uri) {
        if (uri == null || uri.isBlank()) {
            return "/login";
        }

        if (uri.startsWith("/simulation/history-page/")) {
            return "/simulation/history-page";
        }
        if (uri.startsWith("/simulation/history-page")) {
            return "/simulation/history-page";
        }
        if (uri.startsWith("/simulation/evaluation-history/")) {
            return "/simulation/evaluation-history";
        }
        if (uri.startsWith("/simulation/advanced-dashboard")) {
            return "/simulation/advanced-dashboard";
        }
        if (uri.startsWith("/simulation/evaluation-history")) {
            return "/simulation/evaluation-history";
        }
        if (uri.startsWith("/simulation/evaluation-compare")) {
            return "/simulation/evaluation-compare";
        }
        if (uri.startsWith("/simulation/evaluation-dashboard")) {
            return "/simulation/evaluation-dashboard";
        }
        if (uri.startsWith("/simulation/compare-page")) {
            return "/simulation/compare-page";
        }
        if (uri.startsWith("/simulation")) {
            return "/simulation/dashboard";
        }
        if (uri.startsWith("/receive") || uri.startsWith("/ui/decrypt")) {
            return "/receive";
        }
        if (uri.startsWith("/send") || uri.startsWith("/ui/send")) {
            return "/send";
        }

        return "/login";
    }
}
