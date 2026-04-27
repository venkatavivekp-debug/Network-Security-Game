package backend.security;

import backend.audit.AuditEvent;
import backend.audit.AuditEventRepository;
import backend.audit.AuditEventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates outside-threat audit signals for the SOC dashboard.
 *
 * <p>Returns counts (rate-limit blocks, forbidden access, validation rejects,
 * session anomalies, puzzle failures) over a rolling window plus a small slice
 * of recent events. Everything is hashed at write time so no raw IPs,
 * fingerprints, or plaintext leak via this card.
 */
@Service
public class ExternalThreatSummaryService {

    private final AuditEventRepository auditEventRepository;
    private final long windowMinutes;

    public ExternalThreatSummaryService(
            AuditEventRepository auditEventRepository,
            @Value("${app.security.threat-summary-window-minutes:60}") long windowMinutes
    ) {
        this.auditEventRepository = auditEventRepository;
        this.windowMinutes = Math.max(1L, windowMinutes);
    }

    public Map<String, Object> summary() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);

        Map<String, Long> counters = new LinkedHashMap<>();
        counters.put("rateLimitBlocked", auditEventRepository.countByTypeSince(AuditEventType.RATE_LIMIT_BLOCKED, since));
        counters.put("forbiddenAccess", auditEventRepository.countByTypeSince(AuditEventType.FORBIDDEN_ACCESS, since));
        counters.put("validationRejected", auditEventRepository.countByTypeSince(AuditEventType.VALIDATION_REJECTED, since));
        counters.put("replayBlocked", auditEventRepository.countByTypeSince(AuditEventType.REPLAY_BLOCKED, since));
        counters.put("tamperRejected", auditEventRepository.countByTypeSince(AuditEventType.INTEGRITY_FAILED, since));
        counters.put("throttleApplied", auditEventRepository.countByTypeSince(AuditEventType.THROTTLE_APPLIED, since));
        counters.put("sessionAnomaly", auditEventRepository.countByTypeSince(AuditEventType.SESSION_ANOMALY, since));
        counters.put("puzzleSolveFailure", auditEventRepository.countByTypeSince(AuditEventType.PUZZLE_SOLVE_FAILURE, since));
        counters.put("loginFailure", auditEventRepository.countByTypeSince(AuditEventType.AUTH_LOGIN_FAILURE, since));

        long blockedRequests = counters.get("rateLimitBlocked")
                + counters.get("forbiddenAccess")
                + counters.get("validationRejected")
                + counters.get("replayBlocked")
                + counters.get("tamperRejected");

        List<Map<String, Object>> recent = new ArrayList<>();
        List<AuditEvent> all = auditEventRepository.findTop200ByOrderByCreatedAtDesc();
        for (AuditEvent ev : all) {
            if (!isExternalThreatEvent(ev.getEventType())) continue;
            if (ev.getCreatedAt() == null || ev.getCreatedAt().isBefore(since)) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", ev.getId());
            entry.put("eventType", ev.getEventType().name());
            entry.put("actor", ev.getActorUsername());
            entry.put("createdAt", ev.getCreatedAt());
            recent.add(entry);
            if (recent.size() >= 25) break;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("windowMinutes", windowMinutes);
        result.put("counters", counters);
        result.put("blockedRequests", blockedRequests);
        result.put("recent", recent);
        return result;
    }

    private boolean isExternalThreatEvent(AuditEventType type) {
        return type == AuditEventType.RATE_LIMIT_BLOCKED
                || type == AuditEventType.FORBIDDEN_ACCESS
                || type == AuditEventType.VALIDATION_REJECTED
                || type == AuditEventType.REPLAY_BLOCKED
                || type == AuditEventType.INTEGRITY_FAILED
                || type == AuditEventType.THROTTLE_APPLIED
                || type == AuditEventType.SESSION_ANOMALY
                || type == AuditEventType.PUZZLE_SOLVE_FAILURE
                || type == AuditEventType.AUTH_LOGIN_FAILURE
                || type == AuditEventType.AUTH_ACCOUNT_LOCKED;
    }
}
