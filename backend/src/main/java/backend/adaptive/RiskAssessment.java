package backend.adaptive;

import java.util.List;

public class RiskAssessment {

    private final double riskScore;
    private final RiskLevel riskLevel;
    private final List<String> signals;

    public RiskAssessment(double riskScore, RiskLevel riskLevel, List<String> signals) {
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.signals = signals;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<String> getSignals() {
        return signals;
    }
}

