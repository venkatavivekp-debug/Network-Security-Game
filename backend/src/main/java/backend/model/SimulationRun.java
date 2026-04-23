package backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_runs")
public class SimulationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "num_nodes", nullable = false)
    private int numNodes;

    @Column(name = "num_edges", nullable = false)
    private int numEdges;

    @Column(name = "attack_budget", nullable = false)
    private int attackBudget;

    @Column(name = "defense_budget", nullable = false)
    private int defenseBudget;

    @Column(name = "recovery_budget", nullable = false)
    private int recoveryBudget;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm_type", nullable = false, length = 20)
    private AlgorithmType algorithmType;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "initial_connectivity", nullable = false)
    private double initialConnectivity;

    @Column(name = "after_attack_connectivity", nullable = false)
    private double afterAttackConnectivity;

    @Column(name = "after_recovery_connectivity", nullable = false)
    private double afterRecoveryConnectivity;

    @Column(name = "nodes_lost", nullable = false)
    private int nodesLost;

    @Column(name = "edges_lost", nullable = false)
    private int edgesLost;

    @Column(name = "recovery_rate", nullable = false)
    private double recoveryRate;

    @Column(name = "defender_utility", nullable = false)
    private double defenderUtility;

    @Column(name = "attacker_utility", nullable = false)
    private double attackerUtility;

    @Column(name = "effective_attack_success_probability", nullable = false)
    private double effectiveAttackSuccessProbability;

    @Column(name = "created_at", nullable = false)
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
