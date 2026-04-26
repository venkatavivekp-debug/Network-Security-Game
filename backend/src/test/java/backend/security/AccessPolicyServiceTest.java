package backend.security;

import backend.exception.ResourceNotFoundException;
import backend.model.Message;
import backend.model.Role;
import backend.model.User;
import backend.repository.MessageRepository;
import backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Object-level authorization checks for the centralized access guard.
 *
 * <p>The same NotFound shape is returned to a non-participant as it is to a
 * caller asking for an id that doesn't exist, so this also serves as an
 * existence-leak regression test (IDOR / API1).
 */
class AccessPolicyServiceTest {

    @Test
    void requireParticipantReturnsMessageWhenSenderMatches() {
        Fixture f = new Fixture();
        User sender = userWith(7L, "alice", Role.SENDER);
        Message msg = participantMessage(42L, sender, userWith(8L, "bob", Role.RECEIVER));
        when(f.userService.getRequiredByUsername("alice")).thenReturn(sender);
        when(f.messageRepository.findByIdAndParticipant(42L, sender)).thenReturn(Optional.of(msg));

        Message resolved = f.policy.requireParticipant(42L, auth("alice", "ROLE_SENDER"));
        assertSame(msg, resolved);
    }

    @Test
    void requireParticipantThrowsNotFoundForCrossReceiver() {
        Fixture f = new Fixture();
        User unrelated = userWith(99L, "eve", Role.RECEIVER);
        when(f.userService.getRequiredByUsername("eve")).thenReturn(unrelated);
        when(f.messageRepository.findByIdAndParticipant(42L, unrelated)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> f.policy.requireParticipant(42L, auth("eve", "ROLE_RECEIVER")));
        assertEquals("Message not found: 42", ex.getMessage(),
                "Cross-receiver access must surface the same NotFound shape as a missing id");
    }

    @Test
    void requireReceiverRejectsSenderRole() {
        Fixture f = new Fixture();
        User sender = userWith(7L, "alice", Role.SENDER);
        when(f.userService.getRequiredByUsername("alice")).thenReturn(sender);
        assertThrows(AccessDeniedException.class,
                () -> f.policy.requireReceiver(42L, auth("alice", "ROLE_SENDER")));
    }

    @Test
    void requireSenderRejectsForeignSender() {
        Fixture f = new Fixture();
        User otherSender = userWith(8L, "trudy", Role.SENDER);
        Message msg = participantMessage(42L,
                userWith(7L, "alice", Role.SENDER),
                userWith(9L, "bob", Role.RECEIVER));
        when(f.userService.getRequiredByUsername("trudy")).thenReturn(otherSender);
        when(f.messageRepository.findById(42L)).thenReturn(Optional.of(msg));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> f.policy.requireSender(42L, auth("trudy", "ROLE_SENDER")));
        assertEquals("Message not found: 42", ex.getMessage(),
                "A non-owner sender must see the same NotFound shape as a missing id");
    }

    @Test
    void requireParticipantRejectsAnonymous() {
        Fixture f = new Fixture();
        assertThrows(AccessDeniedException.class,
                () -> f.policy.requireParticipant(42L, null));
    }

    private static Authentication auth(String username, String authority) {
        return new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority(authority))
        );
    }

    private static User userWith(Long id, String username, Role role) {
        User u = new User(username, "x", role);
        u.setId(id);
        return u;
    }

    private static Message participantMessage(Long id, User sender, User receiver) {
        Message m = new Message();
        m.setId(id);
        m.setSender(sender);
        m.setReceiver(receiver);
        return m;
    }

    private static class Fixture {
        final MessageRepository messageRepository = mock(MessageRepository.class);
        final UserService userService = mock(UserService.class);
        final AccessPolicyService policy = new AccessPolicyService(messageRepository, userService);
    }
}
