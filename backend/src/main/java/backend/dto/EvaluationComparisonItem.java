package backend.dto;

import backend.model.AlgorithmType;

public class EvaluationComparisonItem {

    private String label;
    private AlgorithmType algorithmType;
    private boolean enableMTD;
    private boolean enableDeception;
    private int attackBudget;
    private int defenseBudget;
    private EvaluationAggregateMetricsResponse aggregateMetrics;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    public boolean isEnableMTD() {
        return enableMTD;
    }

    public void setEnableMTD(boolean enableMTD) {
        this.enableMTD = enableMTD;
    }

    public boolean isEnableDeception() {
        return enableDeception;
    }

    public void setEnableDeception(boolean enableDeception) {
        this.enableDeception = enableDeception;
    }

    public int getAttackBudget() {
        return attackBudget;
    }

    public void setAttackBudget(int attackBudget) {
        this.attackBudget = attackBudget;
    }

    public int getDefenseBudget() {
        return defenseBudget;
    }

    public void setDefenseBudget(int defenseBudget) {
        this.defenseBudget = defenseBudget;
    }

    public EvaluationAggregateMetricsResponse getAggregateMetrics() {
        return aggregateMetrics;
    }

    public void setAggregateMetrics(EvaluationAggregateMetricsResponse aggregateMetrics) {
        this.aggregateMetrics = aggregateMetrics;
    }
}
