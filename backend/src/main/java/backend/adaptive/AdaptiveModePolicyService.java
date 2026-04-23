package backend.adaptive;

import backend.config.PuzzleProperties;
import backend.model.AlgorithmType;
import backend.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdaptiveModePolicyService {

    private final AdaptiveSecurityService adaptiveSecurityService;
    private final ThreatSignalService threatSignalService;
    private final AdaptiveSecurityProperties adaptiveSecurityProperties;
    private final PuzzleProperties puzzleProperties;

    public AdaptiveModePolicyService(
            AdaptiveSecurityService adaptiveSecurityService,
            ThreatSignalService threatSignalService,
            AdaptiveSecurityProperties adaptiveSecurityProperties,
            PuzzleProperties puzzleProperties
    ) {
        this.adaptiveSecurityService = adaptiveSecurityService;
        this.threatSignalService = threatSignalService;
        this.adaptiveSecurityProperties = adaptiveSecurityProperties;
        this.puzzleProperties = puzzleProperties;
    }

    public AdaptiveDecision decide(User sender, AlgorithmType requestedMode, String ip, String userAgent, int recentPuzzleFailures, int recentEscalations) {
        RiskAssessment assessment = adaptiveSecurityService.assess(sender, ip, userAgent, recentPuzzleFailures);
        double threat = threatSignalService.currentAttackIntensity01();

        List<String> reasons = new ArrayList<>();
        if (!assessment.getSignals().isEmpty()) reasons.addAll(assessment.getSignals());
        if (recentEscalations > 0) reasons.add("recent_escalations=" + recentEscalations);

        AlgorithmType effective = requestedMode;
        boolean hold = false;

        // Reliability-first: prefer step-up controls before hard denial.
        if (assessment.getRiskLevel() == RiskLevel.LOW) {
            effective = requestedMode;
        } else if (assessment.getRiskLevel() == RiskLevel.ELEVATED) {
            // Prefer stronger posture if the user asked for NORMAL.
            if (requestedMode == AlgorithmType.NORMAL && (threat > 0.55 || assessment.getRiskScore() > 0.55)) {
                effective = AlgorithmType.SHCS;
                reasons.add("step_up_to_shcs");
            }
        } else if (assessment.getRiskLevel() == RiskLevel.HIGH) {
            // High risk: enforce CPHS gating for meaningful proof-of-legitimacy.
            if (requestedMode != AlgorithmType.CPHS) {
                effective = AlgorithmType.CPHS;
                reasons.add("enforced_cphs_high_risk");
            }
        } else if (assessment.getRiskLevel() == RiskLevel.CRITICAL) {
            // Critical: enforce CPHS and apply a temporary communication hold for admin-supervised recovery.
            effective = AlgorithmType.CPHS;
            hold = true;
            reasons.add("admin_review_required");
        }

        boolean escalated = effective != requestedMode;
        PuzzleDifficulty difficulty = computeDifficulty(assessment, threat, recentPuzzleFailures);

        return new AdaptiveDecision(requestedMode, effective, escalated, hold, reasons, assessment, difficulty);
    }

    private PuzzleDifficulty computeDifficulty(RiskAssessment assessment, double threat, int recentPuzzleFailures) {
        int serverMax = Math.max(1, puzzleProperties.getMaxIterations());
        int base = Math.max(20_000, Math.min(serverMax, 60_000));

        double score = assessment.getRiskScore();
        double factor;
        if (assessment.getRiskLevel() == RiskLevel.LOW) factor = 1.0;
        else if (assessment.getRiskLevel() == RiskLevel.ELEVATED) factor = 1.4;
        else if (assessment.getRiskLevel() == RiskLevel.HIGH) factor = 2.2;
        else factor = 2.8;

        factor *= (1.0 + 0.6 * clamp01(threat));
        factor *= (1.0 + 0.20 * Math.min(6, Math.max(0, recentPuzzleFailures)));
        factor *= (1.0 + 0.25 * clamp01(score));

        int maxIterations = (int) Math.round(base * factor);
        maxIterations = Math.max(15_000, Math.min(serverMax, maxIterations));

        int attemptsAllowed;
        int timeLimitSeconds;

        // Usability guardrails: fewer attempts at higher risk, but never 0; time limit stays humane.
        if (assessment.getRiskLevel() == RiskLevel.LOW) {
            attemptsAllowed = 3;
            timeLimitSeconds = 300;
        } else if (assessment.getRiskLevel() == RiskLevel.ELEVATED) {
            attemptsAllowed = 3;
            timeLimitSeconds = 240;
        } else if (assessment.getRiskLevel() == RiskLevel.HIGH) {
            attemptsAllowed = 2;
            timeLimitSeconds = 210;
        } else {
            attemptsAllowed = 2;
            timeLimitSeconds = 180;
        }

        // Bounded by server settings to prevent misconfiguration making puzzles unusable.
        attemptsAllowed = Math.max(1, Math.min(5, attemptsAllowed));
        timeLimitSeconds = Math.max(60, Math.min(900, timeLimitSeconds));

        return new PuzzleDifficulty(maxIterations, attemptsAllowed, timeLimitSeconds);
    }

    private double clamp01(double v) {
        if (!Double.isFinite(v)) return 0;
        return Math.max(0, Math.min(1, v));
    }
}

