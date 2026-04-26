package backend.controller;

import backend.adaptive.AdaptiveSecurityProperties;
import backend.adaptive.AdaptiveSecurityService;
import backend.adaptive.SystemPressureService;
import backend.adaptive.ThreatSignalService;
import backend.adaptive.UserBehaviorProfileService;
import backend.audit.AuditEventRepository;
import backend.audit.AuditService;
import backend.model.Message;
import backend.model.MessageStatus;
import backend.model.Role;
import backend.model.User;
import backend.model.UserBehaviorProfile;
import backend.repository.MessageRepository;
import backend.repository.PuzzleRepository;
import backend.repository.UserBehaviorProfileRepository;
import backend.security.RecoveryPolicyService;
import backend.service.UserService;
import backend.util.HashUtil;
import backend.util.RequestContextUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminControllerTest {

    @Test
    void holdAndReleaseMessageShouldTransitionStatus() {
        Fixture f = new Fixture();
        User receiver = new User("r", "x", Role.RECEIVER);
        receiver.setId(11L);
        Message msg = new Message();
        msg.setId(5L);
        msg.setReceiver(receiver);
        msg.setStatus(MessageStatus.LOCKED);

        when(f.messageRepository.findById(5L)).thenReturn(Optional.of(msg));
        when(f.messageRepository.save(msg)).thenReturn(msg);
        when(f.behaviorRepository.findByUserId(11L))
                .thenReturn(Optional.of(new UserBehaviorProfile(11L)));

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/admin/hold-message");
        f.controller.holdMessage(5L, "ADMIN_REVIEW_REQUIRED",
                new TestingAuthenticationToken("admin", "x", "ROLE_ADMIN"), req);
        assertEquals(MessageStatus.HELD, msg.getStatus());

        req.setRequestURI("/admin/release-message");
        f.controller.releaseMessage(5L,
                new TestingAuthenticationToken("admin", "x", "ROLE_ADMIN"), req);
        assertEquals(MessageStatus.LOCKED, msg.getStatus());
    }

    @Test
    void heldMessagesEndpointMustNeverIncludeCiphertextOrPlaintext() {
        Fixture f = new Fixture();
        User receiver = new User("r", "x", Role.RECEIVER);
        receiver.setId(11L);
        Message msg = new Message();
        msg.setId(5L);
        msg.setReceiver(receiver);
        msg.setStatus(MessageStatus.HELD);
        msg.setEncryptedContent("ENCRYPTED_BLOB_xyz");
        msg.setHoldReason("PUZZLE_ATTEMPTS_EXHAUSTED");

        when(f.messageRepository.findAll()).thenReturn(List.of(msg));
        when(f.puzzleRepository.findByMessageId(5L)).thenReturn(Optional.empty());

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/admin/held-messages");
        var body = f.controller.heldMessages(req).getBody().getData();
        assertEquals(1, body.size());
        Map<String, Object> entry = body.get(0);
        for (Object value : entry.values()) {
            if (value == null) continue;
            assertFalse(value.toString().contains("ENCRYPTED_BLOB"),
                    "admin endpoint must not return ciphertext content");
        }
    }

    private static class Fixture {
        final MessageRepository messageRepository = mock(MessageRepository.class);
        final PuzzleRepository puzzleRepository = mock(PuzzleRepository.class);
        final UserBehaviorProfileRepository behaviorRepository = mock(UserBehaviorProfileRepository.class);
        final AdminController controller;

        Fixture() {
            UserService userService = mock(UserService.class);
            AuditService auditService = mock(AuditService.class);
            RequestContextUtil requestContextUtil = new RequestContextUtil();
            ThreatSignalService threatSignalService = new ThreatSignalService();
            AuditEventRepository auditEventRepository = mock(AuditEventRepository.class);
            AdaptiveSecurityService adaptiveSecurityService = new AdaptiveSecurityService(
                    new AdaptiveSecurityProperties(), new HashUtil(), auditService, threatSignalService);
            UserBehaviorProfileService behaviorService = new UserBehaviorProfileService(behaviorRepository);
            when(behaviorRepository.save(any(UserBehaviorProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of());
            when(behaviorRepository.findTop50ByOrderByConsecutiveFailuresDescLastFailureAtDesc())
                    .thenReturn(List.of());
            SystemPressureService systemPressureService = new SystemPressureService(
                    threatSignalService, auditEventRepository, behaviorRepository);
            this.controller = new AdminController(
                    userService,
                    adaptiveSecurityService,
                    auditService,
                    requestContextUtil,
                    threatSignalService,
                    auditEventRepository,
                    messageRepository,
                    puzzleRepository,
                    behaviorRepository,
                    behaviorService,
                    systemPressureService,
                    new RecoveryPolicyService()
            );
        }
    }
}
