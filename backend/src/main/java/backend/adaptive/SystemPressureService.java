package backend.adaptive;

import backend.audit.AuditEvent;
import backend.audit.AuditEventRepository;
import backend.audit.AuditEventType;
import backend.repository.UserBehaviorProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates the live signals that connect the messaging engine, the recovery
 * state machine and the simulation battlefield into a single number — the
 * "system pressure". The score is bounded in [0, 1] and is derived from:
 *
 * <ul>
 *   <li>The admin-controlled threat level (attack intensity).</li>
 *   <li>Recent puzzle failure rate observed in the audit log.</li>
 *   <li>Number of users currently in a high-failure state.</li>
 *   <li>Number of messages currently held for admin review.</li>
 * </ul>
 *
 * Both the messaging adaptive engine and the simulation page read this value
 * so that the platform feels coherent: a spike in puzzle failures raises the
 * simulated attack pressure on the network view, and a high admin threat
 * level raises the puzzle difficulty.
 */
@Service
public class SystemPressureService {

    private final ThreatSignalService threatSignalService;
    private final AuditEventRepository auditEventRepository;
    private final UserBehaviorProfileRepository behaviorRepository;

    public SystemPressureService(
            ThreatSignalService threatSignalService,
            AuditEventRepository auditEventRepository,
            UserBehaviorProfileRepository behaviorRepository
    ) {
        this.threatSignalService = threatSignalService;
        this.auditEventRepository = auditEventRepository;
        this.behaviorRepository = behaviorRepository;
    }

    public Snapshot snapshot() {
        double threat = threatSignalService.currentAttackIntensity01();

        List<AuditEvent> recent = auditEventRepository.findTop200ByOrderByCreatedAtDesc();
        LocalDateTime since = LocalDateTime.now().minusMinutes(15);
        long puzzleAttempts = 0;
        long puzzleFailures = 0;
        long heldEvents = 0;
        long adminActions = 0;
        for (AuditEvent event : recent) {
            if (event.getCreatedAt() == null || event.getCreatedAt().isBefore(since)) continue;
            AuditEventType t = event.getEventType();
            if (t == AuditEventType.PUZZLE_SOLVE_SUCCESS) puzzleAttempts++;
            if (t == AuditEventType.PUZZLE_SOLVE_FAILURE) {
                puzzleAttempts++;
                puzzleFailures++;
            }
            if (t == AuditEventType.ADAPTIVE_ESCALATION) heldEvents++;
            if (t == AuditEventType.ADMIN_ACTION) adminActions++;
        }
        double failureRate = puzzleAttempts == 0 ? 0.0 : (double) puzzleFailures / (double) puzzleAttempts;

        long usersAtRisk = behaviorRepository.findTop50ByOrderByConsecutiveFailuresDescLastFailureAtDesc()
                .stream().filter(p -> p.getConsecutiveFailures() >= 2).count();
        double riskUserPressure = clamp01(usersAtRisk / 6.0);

        // Weighted blend; weights chosen so that the threat slider can dominate when admins escalate
        // but the system still reacts on its own when puzzles are failing in the field.
        double pressure = clamp01(
                0.50 * threat +
                0.30 * clamp01(failureRate) +
                0.20 * riskUserPressure
        );
        String level;
        if (pressure < 0.30) level = "CALM";
        else if (pressure < 0.55) level = "WATCH";
        else if (pressure < 0.80) level = "ELEVATED";
        else level = "CRITICAL";

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("threatLevel", threat);
        details.put("recentPuzzleAttempts", puzzleAttempts);
        details.put("recentPuzzleFailures", puzzleFailures);
        details.put("recentEscalations", heldEvents);
        details.put("recentAdminActions", adminActions);
        details.put("usersAtRisk", usersAtRisk);
        details.put("puzzleFailureRate", failureRate);

        return new Snapshot(pressure, level, details);
    }

    private static double clamp01(double v) {
        if (!Double.isFinite(v)) return 0;
        return Math.max(0, Math.min(1, v));
    }

    public record Snapshot(double pressure, String level, Map<String, Object> details) {}
}
