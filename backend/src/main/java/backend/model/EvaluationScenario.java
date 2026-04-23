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
@Table(name = "evaluation_scenarios")
public class EvaluationScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_name", nullable = false, length = 120)
    private String scenarioName;

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

    @Column(name = "rounds", nullable = false)
    private int rounds;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm_type", nullable = false, length = 20)
    private AlgorithmType algorithmType;

    @Column(name = "enable_mtd", nullable = false)
    private boolean enableMtd;

    @Column(name = "enable_deception", nullable = false)
    private boolean enableDeception;

    @Column(name = "repetitions", nullable = false)
    private int repetitions;

    @Enumerated(EnumType.STRING)
    @Column(name = "seed_strategy", nullable = false, length = 20)
    private EvaluationSeedStrategy seedStrategy;

    @Column(name = "base_seed")
    private Long baseSeed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isEnableMtd() {
        return enableMtd;
    }

    public void setEnableMtd(boolean enableMtd) {
        this.enableMtd = enableMtd;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
