package backend.simulation.game;

import backend.config.GameSimulationProperties;

import java.util.Comparator;
import java.util.List;

public class DefenseStrategy {

    private final GameSimulationProperties properties;

    public DefenseStrategy(GameSimulationProperties properties) {
        this.properties = properties;
    }

    public DefenseStageResult apply(NetworkGraph graph, int defenseBudget) {
        double remainingBudget = defenseBudget;
        double spent = 0.0;
        int protectedNodes = 0;
        int protectedEdges = 0;

        List<NetworkNode> nodeCandidates = graph.getNodes().stream()
                .sorted(Comparator.comparingInt((NetworkNode node) -> graph.degreeOf(node.getId(), true)).reversed())
                .toList();

        for (NetworkNode node : nodeCandidates) {
            if (remainingBudget + 1e-9 < properties.getDefenseNodeCost()) {
                break;
            }
            if (!node.isActive() || node.isDefended()) {
                continue;
            }

            node.setDefended(true);
            protectedNodes++;
            spent += properties.getDefenseNodeCost();
            remainingBudget -= properties.getDefenseNodeCost();
        }

        List<NetworkEdge> edgeCandidates = graph.getEdges().stream()
                .sorted(Comparator.comparingInt((NetworkEdge edge) ->
                        graph.degreeOf(edge.getFromNodeId(), true) + graph.degreeOf(edge.getToNodeId(), true)).reversed())
                .toList();

        for (NetworkEdge edge : edgeCandidates) {
            if (remainingBudget + 1e-9 < properties.getDefenseEdgeCost()) {
                break;
            }
            if (!edge.isActive() || edge.isDefended()) {
                continue;
            }

            edge.setDefended(true);
            protectedEdges++;
            spent += properties.getDefenseEdgeCost();
            remainingBudget -= properties.getDefenseEdgeCost();
        }

        return new DefenseStageResult(protectedNodes, protectedEdges, spent);
    }

    public static class DefenseStageResult {
        private final int protectedNodes;
        private final int protectedEdges;
        private final double defenseCost;

        public DefenseStageResult(int protectedNodes, int protectedEdges, double defenseCost) {
            this.protectedNodes = protectedNodes;
            this.protectedEdges = protectedEdges;
            this.defenseCost = defenseCost;
        }

        public int getProtectedNodes() {
            return protectedNodes;
        }

        public int getProtectedEdges() {
            return protectedEdges;
        }

        public double getDefenseCost() {
            return defenseCost;
        }
    }
}
