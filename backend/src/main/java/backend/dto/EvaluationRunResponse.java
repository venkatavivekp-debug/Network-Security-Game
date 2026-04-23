package backend.dto;

import backend.model.AlgorithmType;
import backend.model.EvaluationComparisonType;
import backend.model.EvaluationSeedStrategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EvaluationRunResponse {

    private Long evaluationRunId;
    private Long scenarioId;
    private EvaluationComparisonType comparisonType;

    private String scenarioName;
    private int numNodes;
    private int numEdges;
    private int attackBudget;
    private int defenseBudget;
    private int recoveryBudget;
    private int rounds;
    private AlgorithmType algorithmType;
    private boolean enableMTD;
    private boolean enableDeception;
    private int repetitions;
    private EvaluationSeedStrategy seedStrategy;
    private Long baseSeed;

    private List<Long> usedSeeds = new ArrayList<>();
    private EvaluationAggregateMetricsResponse aggregateMetrics;

    private LocalDateTime createdAt;

    public Long getEvaluationRunId() {
        return evaluationRunId;
    }

    public void setEvaluationRunId(Long evaluationRunId) {
        this.evaluationRunId = evaluationRunId;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public EvaluationComparisonType getComparisonType() {
        return comparisonType;
    }

    public void setComparisonType(EvaluationComparisonType comparisonType) {
        this.comparisonType = comparisonType;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }

    public int getNumEdges() {
        return numEdges;
    }

    public void setNumEdges(int numEdges) {
        this.numEdges = numEdges;
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

    public int getRecoveryBudget() {
        return recoveryBudget;
    }

    public void setRecoveryBudget(int recoveryBudget) {
        this.recoveryBudget = recoveryBudget;
    }

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
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

    public int getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public EvaluationSeedStrategy getSeedStrategy() {
        return seedStrategy;
    }

    public void setSeedStrategy(EvaluationSeedStrategy seedStrategy) {
        this.seedStrategy = seedStrategy;
    }

    public Long getBaseSeed() {
        return baseSeed;
    }

    public void setBaseSeed(Long baseSeed) {
        this.baseSeed = baseSeed;
    }

    public List<Long> getUsedSeeds() {
        return usedSeeds;
    }

    public void setUsedSeeds(List<Long> usedSeeds) {
        this.usedSeeds = usedSeeds;
    }

    public EvaluationAggregateMetricsResponse getAggregateMetrics() {
        return aggregateMetrics;
    }

    public void setAggregateMetrics(EvaluationAggregateMetricsResponse aggregateMetrics) {
        this.aggregateMetrics = aggregateMetrics;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
