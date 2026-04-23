package backend.adaptive;

import backend.model.AlgorithmType;

import java.util.List;

public class AdaptiveDecision {

    private final AlgorithmType requestedMode;
    private final AlgorithmType effectiveMode;
    private final boolean escalated;
    private final boolean communicationHold;
    private final List<String> reasons;
    private final RiskAssessment assessment;
    private final PuzzleDifficulty difficulty;

    public AdaptiveDecision(
            AlgorithmType requestedMode,
            AlgorithmType effectiveMode,
            boolean escalated,
            boolean communicationHold,
            List<String> reasons,
            RiskAssessment assessment,
            PuzzleDifficulty difficulty
    ) {
        this.requestedMode = requestedMode;
        this.effectiveMode = effectiveMode;
        this.escalated = escalated;
        this.communicationHold = communicationHold;
        this.reasons = reasons;
        this.assessment = assessment;
        this.difficulty = difficulty;
    }

    public AlgorithmType getRequestedMode() {
        return requestedMode;
    }

    public AlgorithmType getEffectiveMode() {
        return effectiveMode;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public boolean isCommunicationHold() {
        return communicationHold;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public RiskAssessment getAssessment() {
        return assessment;
    }

    public PuzzleDifficulty getDifficulty() {
        return difficulty;
    }
}

