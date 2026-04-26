package backend.security;

import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.model.Role;
import backend.model.User;
import backend.util.HashUtil;
import backend.util.RequestContextUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ConnectionSecurityServiceTest {

    @Test
    void firstSeenFingerprintDoesNotEmitAnomaly() {
        AuditService audit = mock(AuditService.class);
        ConnectionSecurityService svc = service(audit);

        User u = new User("alice", "x", Role.RECEIVER);
        ConnectionSecurityService.Evaluation eval = svc.evaluate(u, "1.2.3.4", "Mozilla/5.0", "en-US", "session-1");

        assertEquals(ConnectionSecurityService.ConnectionSecurityState.FIRST_SEEN, eval.state());
        assertNotNull(eval.fingerprintHash());
        assertNotNull(eval.snapshot());
        assertEquals(0, eval.snapshot().anomalyCount());
        verify(audit, never()).record(eq(AuditEventType.SESSION_ANOMALY),
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void repeatedExactRequestIsStable() {
        AuditService audit = mock(AuditService.class);
        ConnectionSecurityService svc = service(audit);

        User u = new User("alice", "x", Role.RECEIVER);
        svc.evaluate(u, "1.2.3.4", "ua", "en-US", "session-1");
        ConnectionSecurityService.Evaluation eval = svc.evaluate(u, "1.2.3.4", "ua", "en-US", "session-1");

        assertEquals(ConnectionSecurityService.ConnectionSecurityState.STABLE, eval.state());
        assertEquals(0, eval.snapshot().anomalyCount());
        verify(audit, never()).record(eq(AuditEventType.SESSION_ANOMALY),
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void singleSignalChangeIsShiftedNotAnomalous() {
        AuditService audit = mock(AuditService.class);
        ConnectionSecurityService svc = service(audit);

        User u = new User("alice", "x", Role.RECEIVER);
        svc.evaluate(u, "1.2.3.4", "ua", "en-US", "session-1");
        ConnectionSecurityService.Evaluation eval = svc.evaluate(u, "9.9.9.9", "ua", "en-US", "session-1");

        assertEquals(ConnectionSecurityService.ConnectionSecurityState.SHIFTED, eval.state());
        assertTrue(eval.shiftedSignals().contains("ip"));
        assertEquals(1, eval.snapshot().anomalyCount());
        verify(audit, never()).record(eq(AuditEventType.SESSION_ANOMALY),
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void multipleSignalChangesAreAnomalousAndAudited() {
        AuditService audit = mock(AuditService.class);
        ConnectionSecurityService svc = service(audit);

        User u = new User("alice", "x", Role.RECEIVER);
        svc.evaluate(u, "1.2.3.4", "uaA", "en-US", "session-1");
        ConnectionSecurityService.Evaluation eval = svc.evaluate(u, "9.9.9.9", "uaB", "en-US", "session-1");

        assertEquals(ConnectionSecurityService.ConnectionSecurityState.ANOMALOUS, eval.state());
        assertTrue(eval.shiftedSignals().contains("ip"));
        assertTrue(eval.shiftedSignals().contains("user_agent"));
        assertTrue(eval.snapshot().anomalyCount() >= 1);
        verify(audit, atLeastOnce()).record(eq(AuditEventType.SESSION_ANOMALY),
                eq("alice"), eq("alice"), eq("9.9.9.9"), eq("uaB"), any(), any());
    }

    @Test
    void repeatedShiftsAccumulateIntoAnomalous() {
        AuditService audit = mock(AuditService.class);
        ConnectionSecurityService svc = service(audit);

        User u = new User("bob", "x", Role.RECEIVER);
        svc.evaluate(u, "1.2.3.4", "ua", "en-US", "session-1");
        // single-signal shifts repeated three times pile up to ANOMALOUS
        svc.evaluate(u, "1.2.3.5", "ua", "en-US", "session-1");
        svc.evaluate(u, "1.2.3.6", "ua", "en-US", "session-1");
        ConnectionSecurityService.Evaluation eval = svc.evaluate(u, "1.2.3.7", "ua", "en-US", "session-1");

        assertEquals(ConnectionSecurityService.ConnectionSecurityState.ANOMALOUS, eval.state());
        assertTrue(eval.snapshot().anomalyCount() >= 3);
    }

    private static ConnectionSecurityService service(AuditService audit) {
        return new ConnectionSecurityService(new HashUtil(), audit, new RequestContextUtil());
    }
}
