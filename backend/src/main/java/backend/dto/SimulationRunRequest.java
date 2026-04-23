package backend.dto;

import backend.model.AlgorithmType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SimulationRunRequest {

    @NotNull(message = "numNodes is required")
    @Min(value = 2, message = "numNodes must be at least 2")
    @Max(value = 300, message = "numNodes must be at most 300")
    private Integer numNodes;

    @NotNull(message = "numEdges is required")
    @Min(value = 0, message = "numEdges cannot be negative")
    @Max(value = 50000, message = "numEdges is too large for this simulation")
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

    private AlgorithmType algorithmType;

    @Min(value = 1, message = "messageId must be positive")
    private Long messageId;

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

    @AssertTrue(message = "numEdges exceeds maximum possible edges for numNodes")
    public boolean isGraphSizeValid() {
        if (numNodes == null || numEdges == null) {
            return true;
        }
        int maxEdges = numNodes * (numNodes - 1) / 2;
        return numEdges <= maxEdges;
    }
}
