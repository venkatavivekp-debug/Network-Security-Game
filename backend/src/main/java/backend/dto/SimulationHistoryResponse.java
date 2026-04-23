package backend.dto;

import backend.model.AlgorithmType;

import java.time.LocalDateTime;

public class SimulationHistoryResponse {

    private Long id;
    private int numNodes;
    private int numEdges;
    private int attackBudget;
    private int defenseBudget;
    private int recoveryBudget;
    private AlgorithmType algorithmType;
    private Long messageId;

    private double initialConnectivity;
    private double afterAttackConnectivity;
    private double afterRecoveryConnectivity;
    private int nodesLost;
    private int edgesLost;
    private double recoveryRate;
    private double defenderUtility;
    private double attackerUtility;
    private double effectiveAttackSuccessProbability;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
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
