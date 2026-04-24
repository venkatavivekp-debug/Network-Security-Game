package backend.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Baseline-oriented labels and a short comparison derived from aggregated metrics for one scenario.
 */
public class EvaluationModeComparisonSummary {

    /**
     * Maps experiment mode keys ({@code NORMAL}, {@code SHCS}, …) to research-style category names.
     */
    private Map<String, String> baselineModeLabels = new LinkedHashMap<>();

    private double attackIntensity;

    /**
     * Mode with the strongest outcome on the primary ranking used for this run (lowest compromise, then resilience, then effort).
     */
    private String bestModeUnderAttackIntensity;

    /**
     * One or two sentences summarizing measured security vs effort ordering for this intensity.
     */
    private String securityVsEffortTradeOff;

    public Map<String, String> getBaselineModeLabels() {
        return baselineModeLabels;
    }

    public void setBaselineModeLabels(Map<String, String> baselineModeLabels) {
        this.baselineModeLabels = baselineModeLabels;
    }

    public double getAttackIntensity() {
        return attackIntensity;
    }

    public void setAttackIntensity(double attackIntensity) {
        this.attackIntensity = attackIntensity;
    }

    public String getBestModeUnderAttackIntensity() {
        return bestModeUnderAttackIntensity;
    }

    public void setBestModeUnderAttackIntensity(String bestModeUnderAttackIntensity) {
        this.bestModeUnderAttackIntensity = bestModeUnderAttackIntensity;
    }

    public String getSecurityVsEffortTradeOff() {
        return securityVsEffortTradeOff;
    }

    public void setSecurityVsEffortTradeOff(String securityVsEffortTradeOff) {
        this.securityVsEffortTradeOff = securityVsEffortTradeOff;
    }
}
