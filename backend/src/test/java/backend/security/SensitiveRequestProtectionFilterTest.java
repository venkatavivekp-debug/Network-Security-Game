package backend.security;

import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.model.Role;
import backend.model.User;
import backend.service.UserService;
import backend.util.RequestContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SensitiveRequestProtectionFilterTest {

    @Test
    void reuseSameNonceIsRejectedAsReplay() throws Exception {
        Fixture f = new Fixture();
        String secretB64 = f.seedSecret();
        String nonce = java.util.UUID.randomUUID().toString();
        String ts = String.valueOf(System.currentTimeMillis());

        MockHttpServletRequest req1 = f.req("/message/decrypt/42", nonce, ts, secretB64, "");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        f.filter.doFilterInternal(req1, res1, chain);
        verify(chain).doFilter(any(), any());

        MockHttpServletRequest req2 = f.req("/message/decrypt/42", nonce, ts, secretB64, "");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        f.filter.doFilterInternal(req2, res2, chain);

        assertEquals(403, res2.getStatus());
        assertTrue(res2.getContentAsString().contains("REPLAY_BLOCKED"));
        verify(chain, never()).doFilter(any(), eq(res2));
        verify(f.auditService).record(eq(AuditEventType.REPLAY_BLOCKED), any(), any(), any(), any(), any(), any());
    }

    @Test
    void tamperedBodyIsRejectedAsIntegrityFailure() throws Exception {
        Fixture f = new Fixture();
        String secretB64 = f.seedSecret();
        String nonce = java.util.UUID.randomUUID().toString();
        String ts = String.valueOf(System.currentTimeMillis());

        // Sign for body A...
        String canonicalA = RequestIntegrityService.canonical("POST", "/puzzle/solve/7", "{\"answer\":\"1\"}", ts, nonce);
        String sigA = RequestIntegrityService.signBase64(secretB64, canonicalA);

        // ...but send body B.
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/puzzle/solve/7");
        req.setSession(f.session);
        req.setContentType("application/json");
        req.setContent("{\"answer\":\"2\"}".getBytes());
        req.addHeader(SensitiveRequestProtectionFilter.HEADER_NONCE, nonce);
        req.addHeader(SensitiveRequestProtectionFilter.HEADER_TS, ts);
        req.addHeader(SensitiveRequestProtectionFilter.HEADER_SIG, sigA);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.filter.doFilterInternal(req, res, chain);

        assertEquals(403, res.getStatus());
        assertTrue(res.getContentAsString().contains("REQUEST_INTEGRITY_FAILED"));
        verify(chain, never()).doFilter(any(), any());
        verify(f.auditService).record(eq(AuditEventType.INTEGRITY_FAILED), any(), any(), any(), any(), any(), any());
    }

    private static class Fixture {
        final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        final SessionIntegrityService sessionIntegrityService = new SessionIntegrityService();
        final ReplayNonceStore nonceStore = new ReplayNonceStore(120);
        final UserService userService = mock(UserService.class);
        final AdaptiveThrottleService throttleService = mock(AdaptiveThrottleService.class);
        final AuditService auditService = mock(AuditService.class);
        final RequestContextUtil requestContextUtil = new RequestContextUtil();
        final MockHttpSession session = new MockHttpSession();

        final SensitiveRequestProtectionFilter filter = new SensitiveRequestProtectionFilter(
                mapper,
                sessionIntegrityService,
                nonceStore,
                userService,
                throttleService,
                auditService,
                requestContextUtil,
                120
        );

        Fixture() {
            when(throttleService.computeDelayMs(any(User.class), any())).thenReturn(0L);
            when(userService.getRequiredByUsername(any())).thenReturn(new User("alice", "x", Role.RECEIVER));
        }

        String seedSecret() {
            byte[] key = new byte[32];
            for (int i = 0; i < key.length; i++) key[i] = (byte) (i + 1);
            String b64 = Base64.getEncoder().encodeToString(key);
            session.setAttribute(SessionIntegrityService.SESSION_ATTR, b64);
            return b64;
        }

        MockHttpServletRequest req(String path, String nonce, String ts, String secretB64, String body) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", path);
            req.setSession(session);
            req.setContentType("application/json");
            if (body != null) req.setContent(body.getBytes());
            String canonical = RequestIntegrityService.canonical("POST", path, body == null ? "" : body, ts, nonce);
            String sig = RequestIntegrityService.signBase64(secretB64, canonical);
            req.addHeader(SensitiveRequestProtectionFilter.HEADER_NONCE, nonce);
            req.addHeader(SensitiveRequestProtectionFilter.HEADER_TS, ts);
            req.addHeader(SensitiveRequestProtectionFilter.HEADER_SIG, sig);
            return req;
        }
    }
}

