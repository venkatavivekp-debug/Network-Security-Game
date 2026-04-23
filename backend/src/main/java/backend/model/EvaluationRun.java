package backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_runs")
public class EvaluationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private EvaluationScenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "comparison_type", nullable = false, length = 40)
    private EvaluationComparisonType comparisonType;

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
    @Column(name = "algorithm_type", length = 20)
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

    @Column(name = "used_seeds_json", nullable = false, columnDefinition = "LONGTEXT")
    private String usedSeedsJson;

    @Column(name = "average_final_compromised_nodes", nullable = false)
    private double averageFinalCompromisedNodes;

    @Column(name = "average_compromise_ratio", nullable = false)
    private double averageCompromiseRatio;

    @Column(name = "average_resilience_score", nullable = false)
    private double averageResilienceScore;

    @Column(name = "average_attack_efficiency", nullable = false)
    private double averageAttackEfficiency;

    @Column(name = "average_defense_efficiency", nullable = false)
    private double averageDefenseEfficiency;

    @Column(name = "average_deception_effectiveness", nullable = false)
    private double averageDeceptionEffectiveness;

    @Column(name = "average_mtd_effectiveness", nullable = false)
    private double averageMtdEffectiveness;

    @Column(name = "average_mean_time_to_compromise", nullable = false)
    private double averageMeanTimeToCompromise;

    @Column(name = "average_attack_path_depth", nullable = false)
    private double averageAttackPathDepth;

    @Column(name = "std_dev_final_compromised_nodes", nullable = false)
    private double stdDevFinalCompromisedNodes;

    @Column(name = "std_dev_compromise_ratio", nullable = false)
    private double stdDevCompromiseRatio;

    @Column(name = "std_dev_resilience_score", nullable = false)
    private double stdDevResilienceScore;

    @Column(name = "std_dev_attack_efficiency", nullable = false)
    private double stdDevAttackEfficiency;

    @Column(name = "std_dev_defense_efficiency", nullable = false)
    private double stdDevDefenseEfficiency;

    @Column(name = "std_dev_mean_time_to_compromise", nullable = false)
    private double stdDevMeanTimeToCompromise;

    @Column(name = "comparison_items_json", columnDefinition = "LONGTEXT")
    private String comparisonItemsJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EvaluationScenario getScenario() {
        return scenario;
    }

    public void setScenario(EvaluationScenario scenario) {
        this.scenario = scenario;
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

    public String getUsedSeedsJson() {
        return usedSeedsJson;
    }

    public void setUsedSeedsJson(String usedSeedsJson) {
        this.usedSeedsJson = usedSeedsJson;
    }

    public double getAverageFinalCompromisedNodes() {
        return averageFinalCompromisedNodes;
    }

    public void setAverageFinalCompromisedNodes(double averageFinalCompromisedNodes) {
        this.averageFinalCompromisedNodes = averageFinalCompromisedNodes;
    }

    public double getAverageCompromiseRatio() {
        return averageCompromiseRatio;
    }

    public void setAverageCompromiseRatio(double averageCompromiseRatio) {
        this.averageCompromiseRatio = averageCompromiseRatio;
    }

    public double getAverageResilienceScore() {
        return averageResilienceScore;
    }

    public void setAverageResilienceScore(double averageResilienceScore) {
        this.averageResilienceScore = averageResilienceScore;
    }

    public double getAverageAttackEfficiency() {
        return averageAttackEfficiency;
    }

    public void setAverageAttackEfficiency(double averageAttackEfficiency) {
        this.averageAttackEfficiency = averageAttackEfficiency;
    }

    public double getAverageDefenseEfficiency() {
        return averageDefenseEfficiency;
    }

    public void setAverageDefenseEfficiency(double averageDefenseEfficiency) {
        this.averageDefenseEfficiency = averageDefenseEfficiency;
    }

    public double getAverageDeceptionEffectiveness() {
        return averageDeceptionEffectiveness;
    }

    public void setAverageDeceptionEffectiveness(double averageDeceptionEffectiveness) {
        this.averageDeceptionEffectiveness = averageDeceptionEffectiveness;
    }

    public double getAverageMtdEffectiveness() {
        return averageMtdEffectiveness;
    }

    public void setAverageMtdEffectiveness(double averageMtdEffectiveness) {
        this.averageMtdEffectiveness = averageMtdEffectiveness;
    }

    public double getAverageMeanTimeToCompromise() {
        return averageMeanTimeToCompromise;
    }

    public void setAverageMeanTimeToCompromise(double averageMeanTimeToCompromise) {
        this.averageMeanTimeToCompromise = averageMeanTimeToCompromise;
    }

    public double getAverageAttackPathDepth() {
        return averageAttackPathDepth;
    }

    public void setAverageAttackPathDepth(double averageAttackPathDepth) {
        this.averageAttackPathDepth = averageAttackPathDepth;
    }

    public double getStdDevFinalCompromisedNodes() {
        return stdDevFinalCompromisedNodes;
    }

    public void setStdDevFinalCompromisedNodes(double stdDevFinalCompromisedNodes) {
        this.stdDevFinalCompromisedNodes = stdDevFinalCompromisedNodes;
    }

    public double getStdDevCompromiseRatio() {
        return stdDevCompromiseRatio;
    }

    public void setStdDevCompromiseRatio(double stdDevCompromiseRatio) {
        this.stdDevCompromiseRatio = stdDevCompromiseRatio;
    }

    public double getStdDevResilienceScore() {
        return stdDevResilienceScore;
    }

    public void setStdDevResilienceScore(double stdDevResilienceScore) {
        this.stdDevResilienceScore = stdDevResilienceScore;
    }

    public double getStdDevAttackEfficiency() {
        return stdDevAttackEfficiency;
    }

    public void setStdDevAttackEfficiency(double stdDevAttackEfficiency) {
        this.stdDevAttackEfficiency = stdDevAttackEfficiency;
    }

    public double getStdDevDefenseEfficiency() {
        return stdDevDefenseEfficiency;
    }

    public void setStdDevDefenseEfficiency(double stdDevDefenseEfficiency) {
        this.stdDevDefenseEfficiency = stdDevDefenseEfficiency;
    }

    public double getStdDevMeanTimeToCompromise() {
        return stdDevMeanTimeToCompromise;
    }

    public void setStdDevMeanTimeToCompromise(double stdDevMeanTimeToCompromise) {
        this.stdDevMeanTimeToCompromise = stdDevMeanTimeToCompromise;
    }

    public String getComparisonItemsJson() {
        return comparisonItemsJson;
    }

    public void setComparisonItemsJson(String comparisonItemsJson) {
        this.comparisonItemsJson = comparisonItemsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
