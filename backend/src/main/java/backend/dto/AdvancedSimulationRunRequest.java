package backend.dto;

import backend.model.AlgorithmType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AdvancedSimulationRunRequest {

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

    @NotNull(message = "enableMTD is required")
    private Boolean enableMTD;

    @NotNull(message = "enableDeception is required")
    private Boolean enableDeception;

    @NotNull(message = "algorithmType is required")
    private AlgorithmType algorithmType;

    @Min(value = 0, message = "seed must be non-negative when provided")
    private Long seed;

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

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    @AssertTrue(message = "numEdges exceeds maximum possible directed edges for numNodes")
    public boolean isGraphSizeValid() {
        if (numNodes == null || numEdges == null) {
            return true;
        }
        int maxEdges = numNodes * (numNodes - 1);
        return numEdges <= maxEdges;
    }
}
