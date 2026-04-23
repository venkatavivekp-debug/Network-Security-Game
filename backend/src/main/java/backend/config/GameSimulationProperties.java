package backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.game")
public class GameSimulationProperties {

    private double defenseNodeCost;
    private double defenseEdgeCost;
    private double attackNodeCost;
    private double attackEdgeCost;
    private double recoveryNodeCost;
    private double recoveryEdgeCost;

    private double baseNodeAttackSuccessProbability;
    private double baseEdgeAttackSuccessProbability;
    private double defendedTargetSuccessPenalty;

    private double normalAttackMultiplier;
    private double shcsAttackMultiplier;
    private double cphsAttackMultiplier;

    private double damageConnectivityWeight;
    private double damageNodeWeight;
    private double damageEdgeWeight;

    public double getDefenseNodeCost() {
        return defenseNodeCost;
    }

    public void setDefenseNodeCost(double defenseNodeCost) {
        this.defenseNodeCost = defenseNodeCost;
    }

    public double getDefenseEdgeCost() {
        return defenseEdgeCost;
    }

    public void setDefenseEdgeCost(double defenseEdgeCost) {
        this.defenseEdgeCost = defenseEdgeCost;
    }

    public double getAttackNodeCost() {
        return attackNodeCost;
    }

    public void setAttackNodeCost(double attackNodeCost) {
        this.attackNodeCost = attackNodeCost;
    }

    public double getAttackEdgeCost() {
        return attackEdgeCost;
    }

    public void setAttackEdgeCost(double attackEdgeCost) {
        this.attackEdgeCost = attackEdgeCost;
    }

    public double getRecoveryNodeCost() {
        return recoveryNodeCost;
    }

    public void setRecoveryNodeCost(double recoveryNodeCost) {
        this.recoveryNodeCost = recoveryNodeCost;
    }

    public double getRecoveryEdgeCost() {
        return recoveryEdgeCost;
    }

    public void setRecoveryEdgeCost(double recoveryEdgeCost) {
        this.recoveryEdgeCost = recoveryEdgeCost;
    }

    public double getBaseNodeAttackSuccessProbability() {
        return baseNodeAttackSuccessProbability;
    }

    public void setBaseNodeAttackSuccessProbability(double baseNodeAttackSuccessProbability) {
        this.baseNodeAttackSuccessProbability = baseNodeAttackSuccessProbability;
    }

    public double getBaseEdgeAttackSuccessProbability() {
        return baseEdgeAttackSuccessProbability;
    }

    public void setBaseEdgeAttackSuccessProbability(double baseEdgeAttackSuccessProbability) {
        this.baseEdgeAttackSuccessProbability = baseEdgeAttackSuccessProbability;
    }

    public double getDefendedTargetSuccessPenalty() {
        return defendedTargetSuccessPenalty;
    }

    public void setDefendedTargetSuccessPenalty(double defendedTargetSuccessPenalty) {
        this.defendedTargetSuccessPenalty = defendedTargetSuccessPenalty;
    }

    public double getNormalAttackMultiplier() {
        return normalAttackMultiplier;
    }

    public void setNormalAttackMultiplier(double normalAttackMultiplier) {
        this.normalAttackMultiplier = normalAttackMultiplier;
    }

    public double getShcsAttackMultiplier() {
        return shcsAttackMultiplier;
    }

    public void setShcsAttackMultiplier(double shcsAttackMultiplier) {
        this.shcsAttackMultiplier = shcsAttackMultiplier;
    }

    public double getCphsAttackMultiplier() {
        return cphsAttackMultiplier;
    }

    public void setCphsAttackMultiplier(double cphsAttackMultiplier) {
        this.cphsAttackMultiplier = cphsAttackMultiplier;
    }

    public double getDamageConnectivityWeight() {
        return damageConnectivityWeight;
    }

    public void setDamageConnectivityWeight(double damageConnectivityWeight) {
        this.damageConnectivityWeight = damageConnectivityWeight;
    }

    public double getDamageNodeWeight() {
        return damageNodeWeight;
    }

    public void setDamageNodeWeight(double damageNodeWeight) {
        this.damageNodeWeight = damageNodeWeight;
    }

    public double getDamageEdgeWeight() {
        return damageEdgeWeight;
    }

    public void setDamageEdgeWeight(double damageEdgeWeight) {
        this.damageEdgeWeight = damageEdgeWeight;
    }
}
