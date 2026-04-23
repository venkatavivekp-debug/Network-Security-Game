package backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class SimulationComparisonResponse {

    private int numNodes;
    private int numEdges;
    private int attackBudget;
    private int defenseBudget;
    private int recoveryBudget;
    private LocalDateTime createdAt;
    private List<SimulationComparisonItem> items;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<SimulationComparisonItem> getItems() {
        return items;
    }

    public void setItems(List<SimulationComparisonItem> items) {
        this.items = items;
    }
}
