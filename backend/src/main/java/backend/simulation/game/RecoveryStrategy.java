package backend.simulation.game;

import backend.config.GameSimulationProperties;

import java.util.Comparator;
import java.util.List;

public class RecoveryStrategy {

    private final GameSimulationProperties properties;

    public RecoveryStrategy(GameSimulationProperties properties) {
        this.properties = properties;
    }

    public RecoveryStageResult apply(NetworkGraph graph, int recoveryBudget) {
        double remainingBudget = recoveryBudget;
        double spent = 0.0;
        int recoveredNodes = 0;
        int recoveredEdges = 0;

        List<NetworkNode> nodeCandidates = graph.getNodes().stream()
                .filter(node -> !node.isActive())
                .sorted(Comparator.comparingInt((NetworkNode node) -> graph.degreeOf(node.getId(), false)).reversed())
                .toList();

        for (NetworkNode node : nodeCandidates) {
            if (remainingBudget + 1e-9 < properties.getRecoveryNodeCost()) {
                break;
            }
            if (graph.recoverNode(node.getId())) {
                recoveredNodes++;
                spent += properties.getRecoveryNodeCost();
                remainingBudget -= properties.getRecoveryNodeCost();
            }
        }

        List<NetworkEdge> edgeCandidates = graph.getEdges().stream()
                .filter(edge -> !edge.isActive())
                .sorted(Comparator
                        .comparing(NetworkEdge::isDefended, Comparator.reverseOrder())
                        .thenComparing((NetworkEdge edge) -> graph.degreeOf(edge.getFromNodeId(), false)
                                + graph.degreeOf(edge.getToNodeId(), false), Comparator.reverseOrder()))
                .toList();

        for (NetworkEdge edge : edgeCandidates) {
            if (remainingBudget + 1e-9 < properties.getRecoveryEdgeCost()) {
                break;
            }
            if (graph.recoverEdge(edge.getFromNodeId(), edge.getToNodeId())) {
                recoveredEdges++;
                spent += properties.getRecoveryEdgeCost();
                remainingBudget -= properties.getRecoveryEdgeCost();
            }
        }

        return new RecoveryStageResult(recoveredNodes, recoveredEdges, spent);
    }

    public static class RecoveryStageResult {
        private final int recoveredNodes;
        private final int recoveredEdges;
        private final double recoveryCost;

        public RecoveryStageResult(int recoveredNodes, int recoveredEdges, double recoveryCost) {
            this.recoveredNodes = recoveredNodes;
            this.recoveredEdges = recoveredEdges;
            this.recoveryCost = recoveryCost;
        }

        public int getRecoveredNodes() {
            return recoveredNodes;
        }

        public int getRecoveredEdges() {
            return recoveredEdges;
        }

        public double getRecoveryCost() {
            return recoveryCost;
        }
    }
}
