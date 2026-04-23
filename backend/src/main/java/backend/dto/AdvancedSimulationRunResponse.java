package backend.dto;

import backend.model.AlgorithmType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdvancedSimulationRunResponse {

    private Long advancedSimulationRunId;
    private int numNodes;
    private int numEdges;
    private int attackBudget;
    private int defenseBudget;
    private int recoveryBudget;
    private int rounds;
    private boolean enableMTD;
    private boolean enableDeception;
    private AlgorithmType algorithmType;
    private long seed;

    private List<Double> compromiseTimeline = new ArrayList<>();
    private List<Integer> compromisedNodeCountPerRound = new ArrayList<>();
    private List<AdvancedRoundDetailResponse> roundDetails = new ArrayList<>();

    private double meanTimeToCompromise;
    private int maxAttackPathDepth;
    private double resilienceScore;
    private double defenseEfficiency;
    private double attackEfficiency;
    private double deceptionEffectiveness;
    private double mtdEffectiveness;
    private double detectionRate;
    private double recoveryContribution;
    private int finalCompromisedNodes;
    private int finalProtectedNodes;
    private double attackerUtility;
    private double defenderUtility;

    private LocalDateTime createdAt;

    public Long getAdvancedSimulationRunId() {
        return advancedSimulationRunId;
    }

    public void setAdvancedSimulationRunId(Long advancedSimulationRunId) {
        this.advancedSimulationRunId = advancedSimulationRunId;
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

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public List<Double> getCompromiseTimeline() {
        return compromiseTimeline;
    }

    public void setCompromiseTimeline(List<Double> compromiseTimeline) {
        this.compromiseTimeline = compromiseTimeline;
    }

    public List<Integer> getCompromisedNodeCountPerRound() {
        return compromisedNodeCountPerRound;
    }

    public void setCompromisedNodeCountPerRound(List<Integer> compromisedNodeCountPerRound) {
        this.compromisedNodeCountPerRound = compromisedNodeCountPerRound;
    }

    public List<AdvancedRoundDetailResponse> getRoundDetails() {
        return roundDetails;
    }

    public void setRoundDetails(List<AdvancedRoundDetailResponse> roundDetails) {
        this.roundDetails = roundDetails;
    }

    public double getMeanTimeToCompromise() {
        return meanTimeToCompromise;
    }

    public void setMeanTimeToCompromise(double meanTimeToCompromise) {
        this.meanTimeToCompromise = meanTimeToCompromise;
    }

    public int getMaxAttackPathDepth() {
        return maxAttackPathDepth;
    }

    public void setMaxAttackPathDepth(int maxAttackPathDepth) {
        this.maxAttackPathDepth = maxAttackPathDepth;
    }

    public double getResilienceScore() {
        return resilienceScore;
    }

    public void setResilienceScore(double resilienceScore) {
        this.resilienceScore = resilienceScore;
    }

    public double getDefenseEfficiency() {
        return defenseEfficiency;
    }

    public void setDefenseEfficiency(double defenseEfficiency) {
        this.defenseEfficiency = defenseEfficiency;
    }

    public double getAttackEfficiency() {
        return attackEfficiency;
    }

    public void setAttackEfficiency(double attackEfficiency) {
        this.attackEfficiency = attackEfficiency;
    }

    public double getDeceptionEffectiveness() {
        return deceptionEffectiveness;
    }

    public void setDeceptionEffectiveness(double deceptionEffectiveness) {
        this.deceptionEffectiveness = deceptionEffectiveness;
    }

    public double getMtdEffectiveness() {
        return mtdEffectiveness;
    }

    public void setMtdEffectiveness(double mtdEffectiveness) {
        this.mtdEffectiveness = mtdEffectiveness;
    }

    public double getDetectionRate() {
        return detectionRate;
    }

    public void setDetectionRate(double detectionRate) {
        this.detectionRate = detectionRate;
    }

    public double getRecoveryContribution() {
        return recoveryContribution;
    }

    public void setRecoveryContribution(double recoveryContribution) {
        this.recoveryContribution = recoveryContribution;
    }

    public int getFinalCompromisedNodes() {
        return finalCompromisedNodes;
    }

    public void setFinalCompromisedNodes(int finalCompromisedNodes) {
        this.finalCompromisedNodes = finalCompromisedNodes;
    }

    public int getFinalProtectedNodes() {
        return finalProtectedNodes;
    }

    public void setFinalProtectedNodes(int finalProtectedNodes) {
        this.finalProtectedNodes = finalProtectedNodes;
    }

    public double getAttackerUtility() {
        return attackerUtility;
    }

    public void setAttackerUtility(double attackerUtility) {
        this.attackerUtility = attackerUtility;
    }

    public double getDefenderUtility() {
        return defenderUtility;
    }

    public void setDefenderUtility(double defenderUtility) {
        this.defenderUtility = defenderUtility;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
