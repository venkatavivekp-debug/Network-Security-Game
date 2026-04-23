package backend.simulation.game;

import backend.model.AlgorithmType;

public class SimulationResult {

    private final double initialConnectivity;
    private final double afterAttackConnectivity;
    private final double afterRecoveryConnectivity;
    private final int nodesLost;
    private final int edgesLost;
    private final double recoveryRate;
    private final double defenderUtility;
    private final double attackerUtility;
    private final AlgorithmType algorithmType;
    private final double effectiveAttackSuccessProbability;

    public SimulationResult(
            double initialConnectivity,
            double afterAttackConnectivity,
            double afterRecoveryConnectivity,
            int nodesLost,
            int edgesLost,
            double recoveryRate,
            double defenderUtility,
            double attackerUtility,
            AlgorithmType algorithmType,
            double effectiveAttackSuccessProbability
    ) {
        this.initialConnectivity = initialConnectivity;
        this.afterAttackConnectivity = afterAttackConnectivity;
        this.afterRecoveryConnectivity = afterRecoveryConnectivity;
        this.nodesLost = nodesLost;
        this.edgesLost = edgesLost;
        this.recoveryRate = recoveryRate;
        this.defenderUtility = defenderUtility;
        this.attackerUtility = attackerUtility;
        this.algorithmType = algorithmType;
        this.effectiveAttackSuccessProbability = effectiveAttackSuccessProbability;
    }

    public double getInitialConnectivity() {
        return initialConnectivity;
    }

    public double getAfterAttackConnectivity() {
        return afterAttackConnectivity;
    }

    public double getAfterRecoveryConnectivity() {
        return afterRecoveryConnectivity;
    }

    public int getNodesLost() {
        return nodesLost;
    }

    public int getEdgesLost() {
        return edgesLost;
    }

    public double getRecoveryRate() {
        return recoveryRate;
    }

    public double getDefenderUtility() {
        return defenderUtility;
    }

    public double getAttackerUtility() {
        return attackerUtility;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public double getEffectiveAttackSuccessProbability() {
        return effectiveAttackSuccessProbability;
    }
}
