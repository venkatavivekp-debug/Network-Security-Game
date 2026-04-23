package backend.simulation.advanced;

import java.util.ArrayList;
import java.util.List;

public class AdvancedSimulationMetrics {

    private final List<Double> compromiseTimeline = new ArrayList<>();
    private final List<Integer> compromisedNodeCountPerRound = new ArrayList<>();
    private final List<RoundMetrics> roundMetrics = new ArrayList<>();

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
    private long seedUsed;

    public List<Double> getCompromiseTimeline() {
        return compromiseTimeline;
    }

    public List<Integer> getCompromisedNodeCountPerRound() {
        return compromisedNodeCountPerRound;
    }

    public List<RoundMetrics> getRoundMetrics() {
        return roundMetrics;
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

    public long getSeedUsed() {
        return seedUsed;
    }

    public void setSeedUsed(long seedUsed) {
        this.seedUsed = seedUsed;
    }
}
