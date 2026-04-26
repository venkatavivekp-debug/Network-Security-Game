package backend.security;

import backend.audit.AuditService;
import backend.exception.AdminStepUpRequiredException;
import backend.model.Role;
import backend.model.User;
import backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminStepUpServiceTest {

    @Test
    void confirmRequiresMatchingPassword() {
        Fixture f = new Fixture(false);
        TestingAuthenticationToken auth = adminAuth();

        assertThrows(AdminStepUpRequiredException.class,
                () -> f.svc.confirm(auth, "wrong", new MockHttpServletRequest()));
    }

    @Test
    void confirmIssuesShortLivedToken() {
        Fixture f = new Fixture(true);
        TestingAuthenticationToken auth = adminAuth();

        AdminStepUpService.StepUpToken token = f.svc.confirm(auth, "good", new MockHttpServletRequest());

        assertNotNull(token);
        assertNotNull(token.token());
        assertEquals(AdminStepUpService.STEP_UP_TTL.toSeconds(),
                java.time.Duration.between(token.issuedAt(), token.expiresAt()).toSeconds());
    }

    @Test
    void assertConfirmedRejectsRequestsWithoutToken() {
        Fixture f = new Fixture(true);
        TestingAuthenticationToken auth = adminAuth();
        f.svc.confirm(auth, "good", new MockHttpServletRequest());

        MockHttpServletRequest noHeader = new MockHttpServletRequest();
        assertThrows(AdminStepUpRequiredException.class, () -> f.svc.assertConfirmed(auth, noHeader));
    }

    @Test
    void assertConfirmedAcceptsValidToken() {
        Fixture f = new Fixture(true);
        TestingAuthenticationToken auth = adminAuth();
        AdminStepUpService.StepUpToken token = f.svc.confirm(auth, "good", new MockHttpServletRequest());

        MockHttpServletRequest withHeader = new MockHttpServletRequest();
        withHeader.addHeader(AdminStepUpService.HEADER_NAME, token.token());

        f.svc.assertConfirmed(auth, withHeader);
    }

    @Test
    void confirmationExpiresAfterTtl() throws Exception {
        Fixture f = new Fixture(true);
        TestingAuthenticationToken auth = adminAuth();
        AdminStepUpService.StepUpToken token = f.svc.confirm(auth, "good", new MockHttpServletRequest());

        // Reach into the in-memory store and rewrite the token to be already expired.
        Field tokensField = AdminStepUpService.class.getDeclaredField("tokens");
        tokensField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, AdminStepUpService.StepUpToken> store =
                (ConcurrentMap<String, AdminStepUpService.StepUpToken>) tokensField.get(f.svc);
        AdminStepUpService.StepUpToken expired = new AdminStepUpService.StepUpToken(
                token.token(), token.issuedAt().minusMinutes(10), LocalDateTime.now().minusSeconds(1));
        store.put("admin", expired);

        MockHttpServletRequest withHeader = new MockHttpServletRequest();
        withHeader.addHeader(AdminStepUpService.HEADER_NAME, token.token());

        assertThrows(AdminStepUpRequiredException.class, () -> f.svc.assertConfirmed(auth, withHeader));
        assertNull(f.svc.status(auth));
    }

    @Test
    void statusReportsActiveConfirmation() {
        Fixture f = new Fixture(true);
        TestingAuthenticationToken auth = adminAuth();
        f.svc.confirm(auth, "good", new MockHttpServletRequest());

        AdminStepUpService.StepUpToken status = f.svc.status(auth);
        assertNotNull(status);
        assertTrue(status.expiresAt().isAfter(LocalDateTime.now()));
    }

    private static TestingAuthenticationToken adminAuth() {
        return new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN");
    }

    private static class Fixture {
        final AdminStepUpService svc;

        Fixture(boolean passwordMatches) {
            UserService userService = mock(UserService.class);
            User user = new User("admin", "hashed", Role.ADMIN);
            when(userService.getRequiredByUsername("admin")).thenReturn(user);
            PasswordEncoder encoder = mock(PasswordEncoder.class);
            when(encoder.matches(any(), any())).thenReturn(passwordMatches);
            AuditService audit = mock(AuditService.class);
            this.svc = new AdminStepUpService(userService, encoder, audit);
        }
    }
}
