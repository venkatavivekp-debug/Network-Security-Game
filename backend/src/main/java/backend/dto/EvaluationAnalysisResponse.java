package backend.dto;

import backend.model.EvaluationMetrics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payload for {@code GET /evaluation/analysis}: measured metrics plus derived interpretation
 * from those numbers only (no fabricated outcomes).
 */
public class EvaluationAnalysisResponse {

    private Map<String, EvaluationMetrics> metrics = new LinkedHashMap<>();
    private List<String> insights;
    private String bestMode;
    private Map<String, String> recommendedModeByThreatLevel = new LinkedHashMap<>();

    public Map<String, EvaluationMetrics> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, EvaluationMetrics> metrics) {
        this.metrics = metrics;
    }

    public List<String> getInsights() {
        return insights;
    }

    public void setInsights(List<String> insights) {
        this.insights = insights;
    }

    public String getBestMode() {
        return bestMode;
    }

    public void setBestMode(String bestMode) {
        this.bestMode = bestMode;
    }

    public Map<String, String> getRecommendedModeByThreatLevel() {
        return recommendedModeByThreatLevel;
    }

    public void setRecommendedModeByThreatLevel(Map<String, String> recommendedModeByThreatLevel) {
        this.recommendedModeByThreatLevel = recommendedModeByThreatLevel;
    }
}
