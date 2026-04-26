package backend.security;

import backend.exception.ResourceNotFoundException;
import backend.model.Message;
import backend.model.Role;
import backend.model.User;
import backend.repository.MessageRepository;
import backend.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Centralized object-level access policy. Controllers and services that take
 * an externally-supplied id (messageId, etc.) should funnel through this
 * service so the same authorization rule lives in one place.
 *
 * <p>Failures are reported as {@link ResourceNotFoundException} when the user
 * is not a participant — we deliberately do not say "you do not own this
 * message" because that itself leaks existence. Role mismatches throw
 * {@link AccessDeniedException} so the global handler emits a clean 403 and
 * the audit layer logs it as a forbidden access attempt.
 */
@Service
public class AccessPolicyService {

    private final MessageRepository messageRepository;
    private final UserService userService;

    public AccessPolicyService(MessageRepository messageRepository, UserService userService) {
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    /**
     * The caller must be either the sender or the receiver of {@code messageId}
     * to read message metadata. Other callers see the same NotFound response
     * an attacker would see for an id that does not exist at all.
     */
    public Message requireParticipant(Long messageId, Authentication authentication) {
        if (messageId == null || messageId <= 0) {
            throw new ResourceNotFoundException("Message not found: " + messageId);
        }
        User user = currentUser(authentication);
        return messageRepository.findByIdAndParticipant(messageId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
    }

    /** The caller must be the receiver of {@code messageId}. */
    public Message requireReceiver(Long messageId, Authentication authentication) {
        if (messageId == null || messageId <= 0) {
            throw new ResourceNotFoundException("Message not found: " + messageId);
        }
        User user = currentUser(authentication);
        if (user.getRole() != Role.RECEIVER) {
            throw new AccessDeniedException("Only RECEIVER role can access this resource");
        }
        return messageRepository.findByIdAndReceiver(messageId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
    }

    /** The caller must be the sender of {@code messageId}. */
    public Message requireSender(Long messageId, Authentication authentication) {
        if (messageId == null || messageId <= 0) {
            throw new ResourceNotFoundException("Message not found: " + messageId);
        }
        User user = currentUser(authentication);
        if (user.getRole() != Role.SENDER) {
            throw new AccessDeniedException("Only SENDER role can access this resource");
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
        if (message.getSender() == null || !message.getSender().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Message not found: " + messageId);
        }
        return message;
    }

    /** Resolves the authenticated user, or throws a clean access-denied. */
    public User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        try {
            return userService.getRequiredByUsername(authentication.getName());
        } catch (RuntimeException ex) {
            throw new AccessDeniedException("Authenticated user does not exist");
        }
    }
}
