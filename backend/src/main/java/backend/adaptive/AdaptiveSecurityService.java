package backend.adaptive;

import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.model.User;
import backend.util.HashUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdaptiveSecurityService {

    private final AdaptiveSecurityProperties props;
    private final HashUtil hashUtil;
    private final AuditService auditService;
    private final ThreatSignalService threatSignalService;

    public AdaptiveSecurityService(
            AdaptiveSecurityProperties props,
            HashUtil hashUtil,
            AuditService auditService,
            ThreatSignalService threatSignalService
    ) {
        this.props = props;
        this.hashUtil = hashUtil;
        this.auditService = auditService;
        this.threatSignalService = threatSignalService;
    }

    public RiskAssessment assess(User user, String ip, String userAgent, int recentPuzzleFailures) {
        double failedAttempts = clamp01((user.getFailedLoginAttempts() + recentPuzzleFailures) / 8.0);
        double unusualLoginTime = computeUnusualLoginTime(user);
        double newDevice = computeNewDevice(user, ip, userAgent);
        double attackIntensity = threatSignalService.currentAttackIntensity01();
        double behaviorDeviation = clamp01(recentPuzzleFailures / 4.0);

        double score =
                props.getWeightFailedAttempts() * failedAttempts +
                props.getWeightUnusualLoginTime() * unusualLoginTime +
                props.getWeightNewDevice() * newDevice +
                props.getWeightAttackIntensity() * attackIntensity +
                props.getWeightBehaviorDeviation() * behaviorDeviation;

        score = clamp01(score);
        RiskLevel level = toLevel(score);

        List<String> signals = new ArrayList<>();
        if (failedAttempts > 0.25) signals.add("failed_attempts");
        if (unusualLoginTime > 0.55) signals.add("unusual_login_time");
        if (newDevice > 0.80) signals.add("new_device");
        if (attackIntensity > 0.55) signals.add("attack_intensity");
        if (behaviorDeviation > 0.30) signals.add("behavior_deviation");

        return new RiskAssessment(score, level, signals);
    }

    public String fingerprint(String ip, String userAgent) {
        String material = (ip == null ? "" : ip) + "|" + (userAgent == null ? "" : userAgent);
        return hashUtil.sha256Hex(material);
    }

    public void recordEscalation(String username, RiskAssessment assessment, String ip, String userAgent, String decision) {
        auditService.record(
                AuditEventType.ADAPTIVE_ESCALATION,
                username,
                username,
                ip,
                userAgent,
                assessment.getRiskScore(),
                Map.of(
                        "riskLevel", assessment.getRiskLevel().name(),
                        "signals", assessment.getSignals(),
                        "decision", decision
                )
        );
    }

    public LocalDateTime lockUntilNowPlusMinutes() {
        return LocalDateTime.now().plusMinutes(Math.max(1, props.getLockMinutesOnCritical()));
    }

    private RiskLevel toLevel(double score) {
        if (score >= props.getCriticalThreshold()) return RiskLevel.CRITICAL;
        if (score >= props.getHighThreshold()) return RiskLevel.HIGH;
        if (score >= props.getElevatedThreshold()) return RiskLevel.ELEVATED;
        return RiskLevel.LOW;
    }

    private double computeUnusualLoginTime(User user) {
        // Research-grade placeholder: compare current hour to last login hour. (Extendable to a richer behavioral model.)
        if (user.getLastLoginAt() == null) return 0.25;
        int lastHour = user.getLastLoginAt().atZone(ZoneId.systemDefault()).getHour();
        int nowHour = LocalDateTime.now().atZone(ZoneId.systemDefault()).getHour();
        int delta = Math.abs(nowHour - lastHour);
        delta = Math.min(delta, 24 - delta);
        return clamp01(delta / 6.0);
    }

    private double computeNewDevice(User user, String ip, String userAgent) {
        if (ip == null && userAgent == null) return 0.15;
        String fp = fingerprint(ip, userAgent);
        if (user.getLastLoginFingerprintHash() == null) return 0.65;
        return user.getLastLoginFingerprintHash().equals(fp) ? 0.0 : 1.0;
    }

    private double clamp01(double v) {
        if (!Double.isFinite(v)) return 0;
        return Math.max(0, Math.min(1, v));
    }
}

