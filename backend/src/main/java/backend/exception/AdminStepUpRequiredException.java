package backend.exception;

/**
 * Thrown when an admin endpoint requires a recent password re-check (step-up)
 * and the request did not present a valid confirmation token.
 */
public class AdminStepUpRequiredException extends RuntimeException {

    public AdminStepUpRequiredException(String message) {
        super(message);
    }
}
