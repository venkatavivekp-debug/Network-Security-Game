package backend.service;

import backend.adaptive.AdaptiveModePolicyService;
import backend.adaptive.AdaptiveSecurityProperties;
import backend.adaptive.AdaptiveSecurityService;
import backend.adaptive.ThreatSignalService;
import backend.adaptive.UserBehaviorProfileService;
import backend.config.PuzzleProperties;
import backend.audit.AuditService;
import backend.exception.BadRequestException;
import backend.exception.ResourceNotFoundException;
import backend.model.AlgorithmType;
import backend.model.Message;
import backend.model.MessageStatus;
import backend.model.Role;
import backend.model.User;
import backend.repository.MessageRepository;
import backend.repository.PuzzleRepository;
import backend.repository.UserBehaviorProfileRepository;
import backend.security.ConnectionSecurityService;
import backend.security.RecoveryPolicyService;
import backend.util.HashUtil;
import backend.util.RequestContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Access-control and held-recovery checks against {@link MessageService#decryptMessage(Long, String)}.
 *
 * <p>The system relies on Spring Security's URL/role gate + the role check inside the
 * service; this test exercises the service-level guards directly:
 *
 * <ul>
 *   <li>SENDER cannot decrypt (role rejected at the service)</li>
 *   <li>One receiver cannot read another receiver's message (repository scoped query)</li>
 *   <li>HELD messages cannot be decrypted (admin recovery is required first)</li>
 *   <li>Releasing a HELD message restores access without revealing plaintext to admin</li>
 * </ul>
 */
class MessageAccessControlTest {

    @Test
    void senderCannotDecrypt() {
        Fixture f = new Fixture();
        User sender = userWithRole(7L, "alice", Role.SENDER);
        when(f.userService.getRequiredByUsername("alice")).thenReturn(sender);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> f.messageService.decryptMessage(42L, "alice"));
        assertEquals("Only users with RECEIVER role can decrypt messages", ex.getMessage());
    }

    @Test
    void receiverCannotAccessAnotherReceiversMessage() {
        Fixture f = new Fixture();
        User receiverA = userWithRole(11L, "bob", Role.RECEIVER);
        when(f.userService.getRequiredByUsername("bob")).thenReturn(receiverA);
        when(f.messageRepository.findByIdAndReceiver(42L, receiverA)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> f.messageService.decryptMessage(42L, "bob"));
    }

    @Test
    void heldMessageCannotBeDecryptedUntilReleased() {
        Fixture f = new Fixture();
        User receiver = userWithRole(12L, "carol", Role.RECEIVER);
        Message held = new Message();
        held.setId(42L);
        held.setReceiver(receiver);
        held.setStatus(MessageStatus.HELD);
        held.setAlgorithmType(AlgorithmType.NORMAL);

        when(f.userService.getRequiredByUsername("carol")).thenReturn(receiver);
        when(f.messageRepository.findByIdAndReceiver(42L, receiver)).thenReturn(Optional.of(held));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> f.messageService.decryptMessage(42L, "carol"));
        assertEquals("Communication is on hold: admin review required", ex.getMessage());
    }

    @Test
    void recoveryPolicyServiceCoversEveryRecoveryStateWithoutDeadEnds() {
        RecoveryPolicyService svc = new RecoveryPolicyService();
        for (backend.model.RecoveryState state : backend.model.RecoveryState.values()) {
            RecoveryPolicyService.RecoveryPolicy policy = svc.policyFor(state);
            // Terminal-good states (NORMAL, RECOVERED) may have no next steps; everything
            // else must offer at least one explicit recovery path.
            if (!policy.terminalGood()) {
                if (policy.nextSteps().isEmpty()) {
                    throw new AssertionError("Recovery state " + state + " has no next steps -- dead end");
                }
            }
        }
    }

    private static User userWithRole(Long id, String username, Role role) {
        User u = new User(username, "x", role);
        u.setId(id);
        return u;
    }

    private static class Fixture {
        final MessageRepository messageRepository = mock(MessageRepository.class);
        final UserService userService = mock(UserService.class);
        final PuzzleRepository puzzleRepository = mock(PuzzleRepository.class);
        final UserBehaviorProfileRepository behaviorRepository = mock(UserBehaviorProfileRepository.class);
        final AuditService auditService = mock(AuditService.class);
        final MessageService messageService;

        Fixture() {
            CryptoService cryptoService = mock(CryptoService.class);
            SHCSService shcsService = mock(SHCSService.class);
            CPHSService cphsService = mock(CPHSService.class);
            MessagePuzzleService puzzleService = mock(MessagePuzzleService.class);
            ThreatSignalService threatSignalService = new ThreatSignalService();
            AdaptiveSecurityService adaptiveSecurityService = new AdaptiveSecurityService(
                    new AdaptiveSecurityProperties(),
                    new HashUtil(),
                    auditService,
                    threatSignalService
            );
            AdaptiveModePolicyService adaptiveModePolicyService = new AdaptiveModePolicyService(
                    adaptiveSecurityService,
                    threatSignalService,
                    new AdaptiveSecurityProperties(),
                    new PuzzleProperties()
            );
            UserBehaviorProfileService behaviorService = new UserBehaviorProfileService(behaviorRepository);
            RequestContextUtil requestContextUtil = new RequestContextUtil();
            ObjectMapper objectMapper = new ObjectMapper();
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
            HashUtil hashUtil = new HashUtil();
            RecoveryPolicyService recoveryPolicyService = new RecoveryPolicyService();
            ConnectionSecurityService connectionSecurityService = new ConnectionSecurityService(
                    adaptiveSecurityService, auditService);

            this.messageService = new MessageService(
                    messageRepository,
                    userService,
                    cryptoService,
                    shcsService,
                    cphsService,
                    puzzleService,
                    puzzleRepository,
                    adaptiveModePolicyService,
                    behaviorService,
                    auditService,
                    requestContextUtil,
                    objectMapper,
                    validator,
                    hashUtil,
                    recoveryPolicyService,
                    connectionSecurityService
            );
        }
    }
}
