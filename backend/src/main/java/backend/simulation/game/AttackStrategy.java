package backend.simulation.game;

import backend.config.GameSimulationProperties;
import backend.model.AlgorithmType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AttackStrategy {

    private final GameSimulationProperties properties;

    public AttackStrategy(GameSimulationProperties properties) {
        this.properties = properties;
    }

    public AttackStageResult apply(NetworkGraph graph, int attackBudget, AlgorithmType algorithmType, Random random) {
        double remainingBudget = attackBudget;
        double spent = 0.0;
        int attemptedTargets = 0;
        double probabilityAccumulator = 0.0;

        double algorithmMultiplier = resolveAttackMultiplier(algorithmType);

        Map<Integer, Integer> activeNodeDegrees = new HashMap<>();
        for (NetworkNode node : graph.getNodes()) {
            if (node.isActive()) {
                activeNodeDegrees.put(node.getId(), graph.degreeOf(node.getId(), true));
            }
        }

        List<NetworkNode> nodeTargets = graph.getNodes().stream()
                .filter(NetworkNode::isActive)
                .sorted(Comparator
                        .comparing(NetworkNode::isDefended)
                        .thenComparing((NetworkNode node) -> activeNodeDegrees.getOrDefault(node.getId(), 0), Comparator.reverseOrder()))
                .toList();

        for (NetworkNode node : nodeTargets) {
            if (remainingBudget + 1e-9 < properties.getAttackNodeCost()) {
                break;
            }

            double successProbability = clampProbability(
                    properties.getBaseNodeAttackSuccessProbability()
                            * algorithmMultiplier
                            * defensePenalty(node.isDefended())
            );

            attemptedTargets++;
            probabilityAccumulator += successProbability;
            spent += properties.getAttackNodeCost();
            remainingBudget -= properties.getAttackNodeCost();

            if (random.nextDouble() <= successProbability) {
                graph.disableNode(node.getId());
            }
        }

        Map<Long, Integer> endpointDegreeSum = new HashMap<>();
        for (NetworkEdge edge : graph.getEdges()) {
            if (edge.isActive()) {
                int from = edge.getFromNodeId();
                int to = edge.getToNodeId();
                endpointDegreeSum.put(
                        edgeKey(from, to),
                        activeNodeDegrees.getOrDefault(from, graph.degreeOf(from, true))
                                + activeNodeDegrees.getOrDefault(to, graph.degreeOf(to, true))
                );
            }
        }

        List<NetworkEdge> edgeTargets = graph.getEdges().stream()
                .filter(NetworkEdge::isActive)
                .sorted(Comparator
                        .comparing(NetworkEdge::isDefended)
                        .thenComparing(
                                (NetworkEdge edge) -> endpointDegreeSum.getOrDefault(edgeKey(edge.getFromNodeId(), edge.getToNodeId()), 0),
                                Comparator.reverseOrder()
                        ))
                .toList();

        for (NetworkEdge edge : edgeTargets) {
            if (remainingBudget + 1e-9 < properties.getAttackEdgeCost()) {
                break;
            }
            if (!edge.isActive()) {
                continue;
            }

            double successProbability = clampProbability(
                    properties.getBaseEdgeAttackSuccessProbability()
                            * algorithmMultiplier
                            * defensePenalty(edge.isDefended())
            );

            attemptedTargets++;
            probabilityAccumulator += successProbability;
            spent += properties.getAttackEdgeCost();
            remainingBudget -= properties.getAttackEdgeCost();

            if (random.nextDouble() <= successProbability) {
                graph.disableEdge(edge.getFromNodeId(), edge.getToNodeId());
            }
        }

        double averageSuccessProbability = attemptedTargets == 0
                ? 0.0
                : probabilityAccumulator / attemptedTargets;

        return new AttackStageResult(
                graph.countCompromisedNodes(),
                graph.countCompromisedEdges(),
                spent,
                averageSuccessProbability
        );
    }

    private double defensePenalty(boolean defended) {
        return defended ? properties.getDefendedTargetSuccessPenalty() : 1.0;
    }

    private double resolveAttackMultiplier(AlgorithmType algorithmType) {
        return switch (algorithmType) {
            case NORMAL -> properties.getNormalAttackMultiplier();
            case SHCS -> properties.getShcsAttackMultiplier();
            case CPHS -> properties.getCphsAttackMultiplier();
            case ADAPTIVE -> (properties.getShcsAttackMultiplier() + properties.getCphsAttackMultiplier()) / 2.0;
        };
    }

    private double clampProbability(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        return Math.min(value, 1.0);
    }

    private static long edgeKey(int fromNodeId, int toNodeId) {
        return (((long) fromNodeId) << 32) ^ (toNodeId & 0xffffffffL);
    }

    public static class AttackStageResult {
        private final int nodesLost;
        private final int edgesLost;
        private final double attackCost;
        private final double effectiveAttackSuccessProbability;

        public AttackStageResult(int nodesLost, int edgesLost, double attackCost, double effectiveAttackSuccessProbability) {
            this.nodesLost = nodesLost;
            this.edgesLost = edgesLost;
            this.attackCost = attackCost;
            this.effectiveAttackSuccessProbability = effectiveAttackSuccessProbability;
        }

        public int getNodesLost() {
            return nodesLost;
        }

        public int getEdgesLost() {
            return edgesLost;
        }

        public double getAttackCost() {
            return attackCost;
        }

        public double getEffectiveAttackSuccessProbability() {
            return effectiveAttackSuccessProbability;
        }
    }
}
