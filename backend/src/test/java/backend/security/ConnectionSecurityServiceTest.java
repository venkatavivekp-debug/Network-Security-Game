package backend.security;

import backend.adaptive.AdaptiveSecurityProperties;
import backend.adaptive.AdaptiveSecurityService;
import backend.adaptive.ThreatSignalService;
import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.model.Role;
import backend.model.User;
import backend.util.HashUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ConnectionSecurityServiceTest {

    @Test
    void firstSeenFingerprintDoesNotEmitAnomaly() {
        AuditService audit = mock(AuditService.class);
        AdaptiveSecurityService adaptive = adaptive(audit);
        ConnectionSecurityService svc = new ConnectionSecurityService(adaptive, audit);

        User u = new User("alice", "x", Role.RECEIVER);
        ConnectionSecurityService.Evaluation eval = svc.evaluate(u, "1.2.3.4", "Mozilla/5.0");

        assertEquals(ConnectionSecurityService.ConnectionSecurityState.FIRST_SEEN, eval.state());
        assertNotNull(eval.fingerprintHash());
        verify(audit, never()).record(eq(AuditEventType.SESSION_ANOMALY),
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void matchingFingerprintIsStable() {
        AuditService audit = mock(AuditService.class);
        AdaptiveSecurityService adaptive = adaptive(audit);
        ConnectionSecurityService svc = new ConnectionSecurityService(adaptive, audit);

        User u = new User("alice", "x", Role.RECEIVER);
        u.setLastLoginFingerprintHash(adaptive.fingerprint("1.2.3.4", "ua"));

        ConnectionSecurityService.Evaluation eval = svc.evaluate(u, "1.2.3.4", "ua");

        assertEquals(ConnectionSecurityService.ConnectionSecurityState.STABLE, eval.state());
        verify(audit, never()).record(eq(AuditEventType.SESSION_ANOMALY),
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void changedFingerprintIsAnomalousAndAudited() {
        AuditService audit = mock(AuditService.class);
        AdaptiveSecurityService adaptive = adaptive(audit);
        ConnectionSecurityService svc = new ConnectionSecurityService(adaptive, audit);

        User u = new User("alice", "x", Role.RECEIVER);
        u.setLastLoginFingerprintHash(adaptive.fingerprint("1.2.3.4", "uaA"));

        ConnectionSecurityService.Evaluation eval = svc.evaluate(u, "9.9.9.9", "uaB");

        assertEquals(ConnectionSecurityService.ConnectionSecurityState.ANOMALOUS, eval.state());
        verify(audit, times(1)).record(eq(AuditEventType.SESSION_ANOMALY),
                eq("alice"), eq("alice"), eq("9.9.9.9"), eq("uaB"), any(), any());
    }

    private static AdaptiveSecurityService adaptive(AuditService audit) {
        return new AdaptiveSecurityService(
                new AdaptiveSecurityProperties(),
                new HashUtil(),
                audit,
                new ThreatSignalService()
        );
    }
}
