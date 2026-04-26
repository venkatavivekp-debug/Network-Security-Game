package backend.security;

import backend.audit.AuditEvent;
import backend.audit.AuditEventRepository;
import backend.audit.AuditEventType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalThreatSummaryServiceTest {

    @Test
    void summaryCombinesCountersAndRecentEvents() {
        AuditEventRepository repo = mock(AuditEventRepository.class);

        when(repo.countByTypeSince(eq(AuditEventType.RATE_LIMIT_BLOCKED), any())).thenReturn(3L);
        when(repo.countByTypeSince(eq(AuditEventType.FORBIDDEN_ACCESS), any())).thenReturn(2L);
        when(repo.countByTypeSince(eq(AuditEventType.VALIDATION_REJECTED), any())).thenReturn(1L);
        when(repo.countByTypeSince(eq(AuditEventType.SESSION_ANOMALY), any())).thenReturn(0L);
        when(repo.countByTypeSince(eq(AuditEventType.PUZZLE_SOLVE_FAILURE), any())).thenReturn(4L);
        when(repo.countByTypeSince(eq(AuditEventType.AUTH_LOGIN_FAILURE), any())).thenReturn(5L);

        AuditEvent recent = new AuditEvent();
        recent.setId(1L);
        recent.setEventType(AuditEventType.RATE_LIMIT_BLOCKED);
        recent.setActorUsername("alice");
        recent.setCreatedAt(LocalDateTime.now());
        when(repo.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of(recent));

        ExternalThreatSummaryService svc = new ExternalThreatSummaryService(repo, 60);
        Map<String, Object> summary = svc.summary();

        assertEquals(60L, summary.get("windowMinutes"));
        assertEquals(6L, summary.get("blockedRequests"));
        @SuppressWarnings("unchecked")
        Map<String, Long> counters = (Map<String, Long>) summary.get("counters");
        assertEquals(3L, counters.get("rateLimitBlocked"));
        assertEquals(5L, counters.get("loginFailure"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) summary.get("recent");
        assertNotNull(events);
        assertEquals(1, events.size());
        assertTrue(events.get(0).containsKey("eventType"));
        assertTrue(events.get(0).containsKey("createdAt"));
    }
}
