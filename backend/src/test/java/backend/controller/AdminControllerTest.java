package backend.controller;

import backend.adaptive.AdaptiveRiskPolicyService;
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
import backend.security.AdminStepUpService;
import backend.security.RecoveryPolicyService;
import backend.service.UserService;
import backend.util.HashUtil;
import backend.util.RequestContextUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import backend.exception.AdminStepUpRequiredException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        TestingAuthenticationToken adminAuth = new TestingAuthenticationToken("admin", "x", "ROLE_ADMIN");
        f.confirmAdmin(adminAuth, req);
        f.controller.holdMessage(5L, "ADMIN_REVIEW_REQUIRED", adminAuth, req);
        assertEquals(MessageStatus.HELD, msg.getStatus());

        req.setRequestURI("/admin/release-message");
        f.controller.releaseMessage(5L, adminAuth, req);
        assertEquals(MessageStatus.LOCKED, msg.getStatus());
    }

    @Test
    void holdMessageRequiresAdminStepUp() {
        Fixture f = new Fixture();
        User receiver = new User("r", "x", Role.RECEIVER);
        receiver.setId(11L);
        Message msg = new Message();
        msg.setId(5L);
        msg.setReceiver(receiver);
        msg.setStatus(MessageStatus.LOCKED);
        when(f.messageRepository.findById(5L)).thenReturn(Optional.of(msg));

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/admin/hold-message");
        TestingAuthenticationToken adminAuth = new TestingAuthenticationToken("admin", "x", "ROLE_ADMIN");

        // No prior /admin/confirm-action -> sensitive endpoint must reject.
        assertThrows(AdminStepUpRequiredException.class,
                () -> f.controller.holdMessage(5L, "ADMIN_REVIEW_REQUIRED", adminAuth, req));
    }

    @Test
    void resetFailuresRequiresAdminStepUp() {
        Fixture f = new Fixture();
        User user = new User("victim", "x", Role.RECEIVER);
        when(f.userService.getRequiredByUsername("victim")).thenReturn(user);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/admin/reset-failures");
        TestingAuthenticationToken adminAuth = new TestingAuthenticationToken("admin", "x", "ROLE_ADMIN");

        assertThrows(AdminStepUpRequiredException.class,
                () -> f.controller.resetFailures("victim", adminAuth, req));
    }

    @Test
    void heldMessagesDashboardDoesNotRequireStepUp() {
        Fixture f = new Fixture();
        when(f.messageRepository.findAll()).thenReturn(List.of());

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/admin/held-messages");
        // No step-up token attached -> read-only dashboards still work.
        var body = f.controller.heldMessages(req).getBody().getData();
        assertEquals(0, body.size());
    }

    @Test
    void riskPolicyEndpointReturnsThresholdsWeightsAndLimitations() {
        Fixture f = new Fixture();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/admin/risk-policy");

        Map<String, Object> body = f.controller.riskPolicy(req).getBody().getData();
        assertEquals("weighted-heuristic", body.get("model"));
        assertNotNull(body.get("thresholds"));
        assertNotNull(body.get("signals"));
        assertNotNull(body.get("levelActions"));
        assertNotNull(body.get("connectionStateContribution"));
        assertNotNull(body.get("limitations"));

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> signals = (java.util.List<Map<String, Object>>) body.get("signals");
        assertFalse(signals.isEmpty());
        for (Map<String, Object> sig : signals) {
            assertNotNull(sig.get("id"));
            assertNotNull(sig.get("weight"));
            assertNotNull(sig.get("explanation"));
        }
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
        final AdminStepUpService adminStepUpService;
        final UserService userService;

        Fixture() {
            this.userService = mock(UserService.class);
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
            PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
            when(passwordEncoder.matches(any(), any())).thenReturn(true);
            User adminUser = new User("admin", "hashed", Role.ADMIN);
            when(userService.getRequiredByUsername("admin")).thenReturn(adminUser);
            this.adminStepUpService = new AdminStepUpService(userService, passwordEncoder, auditService);
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
                    new RecoveryPolicyService(),
                    adminStepUpService,
                    new AdaptiveRiskPolicyService(new AdaptiveSecurityProperties()),
                    new backend.security.ExternalThreatSummaryService(auditEventRepository, 60)
            );
        }

        /** Mint a step-up token and attach it to the request as the X-Admin-Confirm header. */
        void confirmAdmin(TestingAuthenticationToken adminAuth, MockHttpServletRequest request) {
            AdminStepUpService.StepUpToken token = adminStepUpService.confirm(adminAuth, "anything", request);
            request.addHeader(AdminStepUpService.HEADER_NAME, token.token());
        }
    }
}
