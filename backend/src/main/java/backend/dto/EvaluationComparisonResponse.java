package backend.dto;

import backend.model.EvaluationComparisonType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EvaluationComparisonResponse {

    private EvaluationComparisonType comparisonType;
    private String scenarioName;
    private int numNodes;
    private int numEdges;
    private int rounds;
    private int repetitions;
    private List<EvaluationComparisonItem> items = new ArrayList<>();
    private LocalDateTime createdAt;

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

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public List<EvaluationComparisonItem> getItems() {
        return items;
    }

    public void setItems(List<EvaluationComparisonItem> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
