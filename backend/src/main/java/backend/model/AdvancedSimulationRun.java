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
@Table(name = "advanced_simulation_runs")
public class AdvancedSimulationRun {

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

    @Column(name = "rounds", nullable = false)
    private int rounds;

    @Column(name = "enable_mtd", nullable = false)
    private boolean enableMtd;

    @Column(name = "enable_deception", nullable = false)
    private boolean enableDeception;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm_type", nullable = false, length = 20)
    private AlgorithmType algorithmType;

    @Column(name = "seed_used", nullable = false)
    private long seedUsed;

    @Column(name = "compromise_timeline_json", nullable = false, columnDefinition = "LONGTEXT")
    private String compromiseTimelineJson;

    @Column(name = "compromised_count_per_round_json", nullable = false, columnDefinition = "LONGTEXT")
    private String compromisedCountPerRoundJson;

    @Column(name = "round_details_json", nullable = false, columnDefinition = "LONGTEXT")
    private String roundDetailsJson;

    @Column(name = "mean_time_to_compromise", nullable = false)
    private double meanTimeToCompromise;

    @Column(name = "max_attack_path_depth", nullable = false)
    private int maxAttackPathDepth;

    @Column(name = "resilience_score", nullable = false)
    private double resilienceScore;

    @Column(name = "defense_efficiency", nullable = false)
    private double defenseEfficiency;

    @Column(name = "attack_efficiency", nullable = false)
    private double attackEfficiency;

    @Column(name = "deception_effectiveness", nullable = false)
    private double deceptionEffectiveness;

    @Column(name = "mtd_effectiveness", nullable = false)
    private double mtdEffectiveness;

    @Column(name = "detection_rate", nullable = false)
    private double detectionRate;

    @Column(name = "recovery_contribution", nullable = false)
    private double recoveryContribution;

    @Column(name = "final_compromised_nodes", nullable = false)
    private int finalCompromisedNodes;

    @Column(name = "final_protected_nodes", nullable = false)
    private int finalProtectedNodes;

    @Column(name = "attacker_utility", nullable = false)
    private double attackerUtility;

    @Column(name = "defender_utility", nullable = false)
    private double defenderUtility;

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

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
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

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    public long getSeedUsed() {
        return seedUsed;
    }

    public void setSeedUsed(long seedUsed) {
        this.seedUsed = seedUsed;
    }

    public String getCompromiseTimelineJson() {
        return compromiseTimelineJson;
    }

    public void setCompromiseTimelineJson(String compromiseTimelineJson) {
        this.compromiseTimelineJson = compromiseTimelineJson;
    }

    public String getCompromisedCountPerRoundJson() {
        return compromisedCountPerRoundJson;
    }

    public void setCompromisedCountPerRoundJson(String compromisedCountPerRoundJson) {
        this.compromisedCountPerRoundJson = compromisedCountPerRoundJson;
    }

    public String getRoundDetailsJson() {
        return roundDetailsJson;
    }

    public void setRoundDetailsJson(String roundDetailsJson) {
        this.roundDetailsJson = roundDetailsJson;
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
