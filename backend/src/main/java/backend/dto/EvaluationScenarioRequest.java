package backend.dto;

import backend.model.AlgorithmType;
import backend.model.EvaluationSeedStrategy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EvaluationScenarioRequest {

    @NotBlank(message = "scenarioName is required")
    @Size(min = 3, max = 120, message = "scenarioName must be between 3 and 120 characters")
    private String scenarioName;

    @NotNull(message = "numNodes is required")
    @Min(value = 2, message = "numNodes must be at least 2")
    @Max(value = 500, message = "numNodes must be at most 500")
    private Integer numNodes;

    @NotNull(message = "numEdges is required")
    @Min(value = 0, message = "numEdges cannot be negative")
    @Max(value = 100000, message = "numEdges is too large")
    private Integer numEdges;

    @NotNull(message = "attackBudget is required")
    @Min(value = 0, message = "attackBudget cannot be negative")
    @Max(value = 10000, message = "attackBudget is too large")
    private Integer attackBudget;

    @NotNull(message = "defenseBudget is required")
    @Min(value = 0, message = "defenseBudget cannot be negative")
    @Max(value = 10000, message = "defenseBudget is too large")
    private Integer defenseBudget;

    @NotNull(message = "recoveryBudget is required")
    @Min(value = 0, message = "recoveryBudget cannot be negative")
    @Max(value = 10000, message = "recoveryBudget is too large")
    private Integer recoveryBudget;

    @NotNull(message = "rounds is required")
    @Min(value = 1, message = "rounds must be at least 1")
    @Max(value = 100, message = "rounds must be at most 100")
    private Integer rounds;

    @NotNull(message = "algorithmType is required")
    private AlgorithmType algorithmType;

    @NotNull(message = "enableMTD is required")
    private Boolean enableMTD;

    @NotNull(message = "enableDeception is required")
    private Boolean enableDeception;

    @NotNull(message = "repetitions is required")
    @Min(value = 1, message = "repetitions must be at least 1")
    @Max(value = 200, message = "repetitions must be at most 200")
    private Integer repetitions;

    @NotNull(message = "seedStrategy is required")
    private EvaluationSeedStrategy seedStrategy;

    @Min(value = 0, message = "baseSeed must be non-negative when provided")
    private Long baseSeed;

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public Integer getNumNodes() {
        return numNodes;
    }

    public void setNumNodes(Integer numNodes) {
        this.numNodes = numNodes;
    }

    public Integer getNumEdges() {
        return numEdges;
    }

    public void setNumEdges(Integer numEdges) {
        this.numEdges = numEdges;
    }

    public Integer getAttackBudget() {
        return attackBudget;
    }

    public void setAttackBudget(Integer attackBudget) {
        this.attackBudget = attackBudget;
    }

    public Integer getDefenseBudget() {
        return defenseBudget;
    }

    public void setDefenseBudget(Integer defenseBudget) {
        this.defenseBudget = defenseBudget;
    }

    public Integer getRecoveryBudget() {
        return recoveryBudget;
    }

    public void setRecoveryBudget(Integer recoveryBudget) {
        this.recoveryBudget = recoveryBudget;
    }

    public Integer getRounds() {
        return rounds;
    }

    public void setRounds(Integer rounds) {
        this.rounds = rounds;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    public Boolean getEnableMTD() {
        return enableMTD;
    }

    public void setEnableMTD(Boolean enableMTD) {
        this.enableMTD = enableMTD;
    }

    public Boolean getEnableDeception() {
        return enableDeception;
    }

    public void setEnableDeception(Boolean enableDeception) {
        this.enableDeception = enableDeception;
    }

    public Integer getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(Integer repetitions) {
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

    @AssertTrue(message = "numEdges exceeds maximum possible directed edges for numNodes")
    public boolean isGraphSizeValid() {
        if (numNodes == null || numEdges == null) {
            return true;
        }
        int maxEdges = numNodes * (numNodes - 1);
        return numEdges <= maxEdges;
    }

    @AssertTrue(message = "seedStrategy=FIXED requires baseSeed and repetitions must be 1")
    public boolean isFixedSeedStrategyValid() {
        if (seedStrategy == null || repetitions == null) {
            return true;
        }
        if (seedStrategy != EvaluationSeedStrategy.FIXED) {
            return true;
        }
        return baseSeed != null && repetitions == 1;
    }
}
