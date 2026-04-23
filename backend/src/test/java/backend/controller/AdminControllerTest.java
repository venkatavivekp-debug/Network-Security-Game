package backend.controller;

import backend.adaptive.AdaptiveSecurityProperties;
import backend.adaptive.AdaptiveSecurityService;
import backend.adaptive.ThreatSignalService;
import backend.audit.AuditEventRepository;
import backend.audit.AuditService;
import backend.model.Message;
import backend.model.MessageStatus;
import backend.model.Role;
import backend.model.User;
import backend.repository.MessageRepository;
import backend.service.UserService;
import backend.util.HashUtil;
import backend.util.RequestContextUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminControllerTest {

    @Test
    void holdAndReleaseMessageShouldTransitionStatus() {
        UserService userService = mock(UserService.class);
        AuditService auditService = mock(AuditService.class);
        RequestContextUtil requestContextUtil = new RequestContextUtil();
        ThreatSignalService threatSignalService = new ThreatSignalService();
        AuditEventRepository auditEventRepository = mock(AuditEventRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);

        AdaptiveSecurityService adaptiveSecurityService = new AdaptiveSecurityService(
                new AdaptiveSecurityProperties(),
                new HashUtil(),
                auditService,
                threatSignalService
        );

        AdminController controller = new AdminController(
                userService,
                adaptiveSecurityService,
                auditService,
                requestContextUtil,
                threatSignalService,
                auditEventRepository,
                messageRepository
        );

        User receiver = new User("r", "x", Role.RECEIVER);
        Message msg = new Message();
        msg.setId(5L);
        msg.setReceiver(receiver);
        msg.setStatus(MessageStatus.LOCKED);

        when(messageRepository.findById(5L)).thenReturn(Optional.of(msg));
        when(messageRepository.save(msg)).thenReturn(msg);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/admin/hold-message");

        controller.holdMessage(5L, "ADMIN_REVIEW_REQUIRED", new TestingAuthenticationToken("admin", "x", "ROLE_ADMIN"), req);
        assertEquals(MessageStatus.HELD, msg.getStatus());

        req.setRequestURI("/admin/release-message");
        controller.releaseMessage(5L, new TestingAuthenticationToken("admin", "x", "ROLE_ADMIN"), req);
        assertEquals(MessageStatus.LOCKED, msg.getStatus());
    }
}

