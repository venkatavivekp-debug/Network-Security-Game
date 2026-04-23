package backend.dto;

import backend.model.AlgorithmType;

import java.time.LocalDateTime;

public class SimulationRunResponse {

    private Long simulationRunId;
    private double initialConnectivity;
    private double afterAttackConnectivity;
    private double afterRecoveryConnectivity;
    private int nodesLost;
    private int edgesLost;
    private double recoveryRate;
    private double defenderUtility;
    private double attackerUtility;
    private AlgorithmType algorithmType;
    private double effectiveAttackSuccessProbability;
    private LocalDateTime createdAt;

    public Long getSimulationRunId() {
        return simulationRunId;
    }

    public void setSimulationRunId(Long simulationRunId) {
        this.simulationRunId = simulationRunId;
    }

    public double getInitialConnectivity() {
        return initialConnectivity;
    }

    public void setInitialConnectivity(double initialConnectivity) {
        this.initialConnectivity = initialConnectivity;
    }

    public double getAfterAttackConnectivity() {
        return afterAttackConnectivity;
    }

    public void setAfterAttackConnectivity(double afterAttackConnectivity) {
        this.afterAttackConnectivity = afterAttackConnectivity;
    }

    public double getAfterRecoveryConnectivity() {
        return afterRecoveryConnectivity;
    }

    public void setAfterRecoveryConnectivity(double afterRecoveryConnectivity) {
        this.afterRecoveryConnectivity = afterRecoveryConnectivity;
    }

    public int getNodesLost() {
        return nodesLost;
    }

    public void setNodesLost(int nodesLost) {
        this.nodesLost = nodesLost;
    }

    public int getEdgesLost() {
        return edgesLost;
    }

    public void setEdgesLost(int edgesLost) {
        this.edgesLost = edgesLost;
    }

    public double getRecoveryRate() {
        return recoveryRate;
    }

    public void setRecoveryRate(double recoveryRate) {
        this.recoveryRate = recoveryRate;
    }

    public double getDefenderUtility() {
        return defenderUtility;
    }

    public void setDefenderUtility(double defenderUtility) {
        this.defenderUtility = defenderUtility;
    }

    public double getAttackerUtility() {
        return attackerUtility;
    }

    public void setAttackerUtility(double attackerUtility) {
        this.attackerUtility = attackerUtility;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    public double getEffectiveAttackSuccessProbability() {
        return effectiveAttackSuccessProbability;
    }

    public void setEffectiveAttackSuccessProbability(double effectiveAttackSuccessProbability) {
        this.effectiveAttackSuccessProbability = effectiveAttackSuccessProbability;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
