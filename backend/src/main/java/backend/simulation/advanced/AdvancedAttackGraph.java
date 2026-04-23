package backend.simulation.advanced;

import backend.exception.BadRequestException;
import backend.model.AlgorithmType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class AdvancedAttackGraph {

    private final Map<Integer, AdvancedNode> nodes = new LinkedHashMap<>();
    private final Map<String, AdvancedEdge> edges = new LinkedHashMap<>();

    public void addNode(AdvancedNode node) {
        nodes.put(node.getId(), node);
    }

    public AdvancedNode removeNode(int nodeId) {
        AdvancedNode removed = nodes.remove(nodeId);
        if (removed == null) {
            return null;
        }

        List<String> keysToRemove = edges.values().stream()
                .filter(edge -> edge.getSourceNodeId() == nodeId || edge.getTargetNodeId() == nodeId)
                .map(AdvancedEdge::key)
                .toList();
        keysToRemove.forEach(edges::remove);

        return removed;
    }

    public void addEdge(AdvancedEdge edge) {
        if (!nodes.containsKey(edge.getSourceNodeId()) || !nodes.containsKey(edge.getTargetNodeId())) {
            throw new BadRequestException("Both source and target nodes must exist before adding an edge");
        }
        if (edge.getSourceNodeId() == edge.getTargetNodeId()) {
            throw new BadRequestException("Self-loop edges are not supported in advanced attack graph");
        }
        edges.put(edge.key(), edge);
    }

    public void addEdge(int sourceNodeId, int targetNodeId, double attackCost, double exploitProbability) {
        addEdge(new AdvancedEdge(sourceNodeId, targetNodeId, attackCost, exploitProbability, true));
    }

    public AdvancedEdge removeEdge(int sourceNodeId, int targetNodeId) {
        return edges.remove(AdvancedEdge.key(sourceNodeId, targetNodeId));
    }

    public AdvancedNode getNode(int id) {
        return nodes.get(id);
    }

    public AdvancedEdge getEdge(int sourceNodeId, int targetNodeId) {
        return edges.get(AdvancedEdge.key(sourceNodeId, targetNodeId));
    }

    public List<AdvancedNode> getNodes() {
        return new ArrayList<>(nodes.values());
    }

    public List<AdvancedEdge> getEdges() {
        return new ArrayList<>(edges.values());
    }

    public List<AdvancedEdge> getEnabledEdges() {
        return edges.values().stream().filter(AdvancedEdge::isEnabled).toList();
    }

    public List<AdvancedNode> getNeighbors(int sourceNodeId) {
        return edges.values().stream()
                .filter(AdvancedEdge::isEnabled)
                .filter(edge -> edge.getSourceNodeId() == sourceNodeId)
                .map(edge -> nodes.get(edge.getTargetNodeId()))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<AdvancedEdge> getOutgoingEdges(int sourceNodeId) {
        return edges.values().stream()
                .filter(AdvancedEdge::isEnabled)
                .filter(edge -> edge.getSourceNodeId() == sourceNodeId)
                .toList();
    }

    public List<AdvancedEdge> getIncomingEdges(int targetNodeId) {
        return edges.values().stream()
                .filter(AdvancedEdge::isEnabled)
                .filter(edge -> edge.getTargetNodeId() == targetNodeId)
                .toList();
    }

    public int countCompromisedNodes() {
        return (int) nodes.values().stream().filter(AdvancedNode::isCompromised).count();
    }

    public int countCompromisedRealNodes() {
        return (int) nodes.values().stream().filter(node -> node.isCompromised() && !node.isHoneypot()).count();
    }

    public int countDetectedNodes() {
        return (int) nodes.values().stream().filter(AdvancedNode::isDetected).count();
    }

    public int countHoneypots() {
        return (int) nodes.values().stream().filter(AdvancedNode::isHoneypot).count();
    }

    public int countProtectedNodes(double threshold) {
        return (int) nodes.values().stream()
                .filter(node -> !node.isCompromised() && node.getDefenseLevel() >= threshold)
                .count();
    }

    public int countRealNodes() {
        return (int) nodes.values().stream().filter(node -> !node.isHoneypot()).count();
    }

    public double computeCompromiseRatio() {
        int realNodes = countRealNodes();
        if (realNodes == 0) {
            return 0.0;
        }
        return (double) countCompromisedRealNodes() / realNodes;
    }

    public double computeEnabledEdgeRatio() {
        if (edges.isEmpty()) {
            return 0.0;
        }
        return (double) getEnabledEdges().size() / edges.size();
    }

    public double computeLargestConnectedComponentRatio() {
        List<Integer> nodeIds = nodes.values().stream()
                .filter(node -> !node.isHoneypot())
                .map(AdvancedNode::getId)
                .toList();

        if (nodeIds.isEmpty()) {
            return 0.0;
        }

        Map<Integer, Set<Integer>> undirected = buildUndirectedAdjacency(nodeIds);
        Set<Integer> visited = new HashSet<>();
        int largest = 0;

        for (Integer nodeId : nodeIds) {
            if (visited.contains(nodeId)) {
                continue;
            }
            int size = bfsSize(nodeId, undirected, visited);
            if (size > largest) {
                largest = size;
            }
        }

        return (double) largest / nodeIds.size();
    }

    public int computeMaxCompromisedPathDepth() {
        Set<Integer> compromisedNodes = nodes.values().stream()
                .filter(AdvancedNode::isCompromised)
                .map(AdvancedNode::getId)
                .collect(Collectors.toSet());

        if (compromisedNodes.isEmpty()) {
            return 0;
        }

        int maxDepth = 1;
        for (Integer start : compromisedNodes) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            Map<Integer, Integer> depth = new HashMap<>();
            queue.add(start);
            depth.put(start, 1);

            while (!queue.isEmpty()) {
                int current = queue.poll();
                int currentDepth = depth.get(current);
                maxDepth = Math.max(maxDepth, currentDepth);

                for (AdvancedEdge edge : getOutgoingEdges(current)) {
                    if (!compromisedNodes.contains(edge.getTargetNodeId())) {
                        continue;
                    }
                    if (depth.containsKey(edge.getTargetNodeId())) {
                        continue;
                    }
                    depth.put(edge.getTargetNodeId(), currentDepth + 1);
                    queue.add(edge.getTargetNodeId());
                }
            }
        }

        return maxDepth;
    }

    public int shuffleEdgeConnectivity(Random random, int maxSwaps) {
        List<AdvancedEdge> candidates = getEnabledEdges();
        if (candidates.size() < 2 || maxSwaps <= 0) {
            return 0;
        }

        int swaps = 0;
        int attempts = 0;
        int maxAttempts = maxSwaps * 8;

        while (swaps < maxSwaps && attempts < maxAttempts) {
            attempts++;
            AdvancedEdge first = candidates.get(random.nextInt(candidates.size()));
            AdvancedEdge second = candidates.get(random.nextInt(candidates.size()));
            if (first == second) {
                continue;
            }

            int a = first.getSourceNodeId();
            int b = first.getTargetNodeId();
            int c = second.getSourceNodeId();
            int d = second.getTargetNodeId();

            if (a == d || c == b || a == c || b == d) {
                continue;
            }

            String proposedOne = AdvancedEdge.key(a, d);
            String proposedTwo = AdvancedEdge.key(c, b);
            if (edges.containsKey(proposedOne) || edges.containsKey(proposedTwo)) {
                continue;
            }

            AdvancedEdge one = new AdvancedEdge(a, d, first.getAttackCost(), first.getExploitProbability(), first.isEnabled());
            AdvancedEdge two = new AdvancedEdge(c, b, second.getAttackCost(), second.getExploitProbability(), second.isEnabled());

            edges.remove(first.key());
            edges.remove(second.key());
            edges.put(one.key(), one);
            edges.put(two.key(), two);

            if (!isValid() || wouldCreateFullyOrphanedNetwork()) {
                edges.remove(one.key());
                edges.remove(two.key());
                edges.put(first.key(), first);
                edges.put(second.key(), second);
                continue;
            }

            swaps++;
            candidates = getEnabledEdges();
        }

        return swaps;
    }

    public int rotateLogicalNodeIdentities(Random random) {
        List<AdvancedNode> candidates = nodes.values().stream()
                .filter(node -> !node.isHoneypot())
                .toList();
        if (candidates.size() < 2) {
            return 0;
        }

        List<Double> vulnerabilities = candidates.stream().map(AdvancedNode::getVulnerabilityScore).collect(Collectors.toList());
        List<Double> defenses = candidates.stream().map(AdvancedNode::getDefenseLevel).collect(Collectors.toList());
        Collections.shuffle(vulnerabilities, random);
        Collections.shuffle(defenses, random);

        for (int i = 0; i < candidates.size(); i++) {
            candidates.get(i).setVulnerabilityScore(vulnerabilities.get(i));
            candidates.get(i).setDefenseLevel(defenses.get(i));
        }
        return candidates.size();
    }

    public int rotateEncryptionModeImpacts(Random random) {
        List<AdvancedNode> candidates = nodes.values().stream()
                .filter(node -> !node.isHoneypot())
                .toList();
        if (candidates.isEmpty()) {
            return 0;
        }

        List<AlgorithmType> modes = new ArrayList<>();
        for (AdvancedNode node : candidates) {
            if (node.getEncryptionModeImpact() != null) {
                modes.add(node.getEncryptionModeImpact());
            }
        }

        if (modes.isEmpty()) {
            return 0;
        }

        Collections.shuffle(modes, random);
        int rotated = 0;
        int modeIndex = 0;
        for (AdvancedNode node : candidates) {
            if (node.getEncryptionModeImpact() == null) {
                continue;
            }
            node.setEncryptionModeImpact(modes.get(modeIndex % modes.size()));
            modeIndex++;
            rotated++;
        }

        return rotated;
    }

    public int disableHighRiskEdges(Random random, int maxDisableCount) {
        if (maxDisableCount <= 0) {
            return 0;
        }

        List<AdvancedEdge> highRisk = getEnabledEdges().stream()
                .sorted(Comparator.comparing(AdvancedEdge::getExploitProbability).reversed())
                .toList();

        int disabled = 0;
        for (AdvancedEdge edge : highRisk) {
            if (disabled >= maxDisableCount) {
                break;
            }
            if (enabledOutDegree(edge.getSourceNodeId()) <= 1 && enabledInDegree(edge.getTargetNodeId()) <= 1) {
                continue;
            }

            edge.setEnabled(false);
            if (wouldCreateFullyOrphanedNetwork()) {
                edge.setEnabled(true);
                continue;
            }
            disabled++;
        }

        return disabled;
    }

    public int enabledOutDegree(int nodeId) {
        return (int) edges.values().stream()
                .filter(AdvancedEdge::isEnabled)
                .filter(edge -> edge.getSourceNodeId() == nodeId)
                .count();
    }

    public int enabledInDegree(int nodeId) {
        return (int) edges.values().stream()
                .filter(AdvancedEdge::isEnabled)
                .filter(edge -> edge.getTargetNodeId() == nodeId)
                .count();
    }

    public boolean isValid() {
        for (AdvancedEdge edge : edges.values()) {
            if (!nodes.containsKey(edge.getSourceNodeId()) || !nodes.containsKey(edge.getTargetNodeId())) {
                return false;
            }
            if (edge.getSourceNodeId() == edge.getTargetNodeId()) {
                return false;
            }
        }
        return true;
    }

    public AdvancedAttackGraph cloneGraph() {
        AdvancedAttackGraph copy = new AdvancedAttackGraph();
        for (AdvancedNode node : nodes.values()) {
            copy.addNode(new AdvancedNode(node));
        }
        for (AdvancedEdge edge : edges.values()) {
            copy.addEdge(new AdvancedEdge(edge));
        }
        return copy;
    }

    public static AdvancedAttackGraph generateRandomGraph(
            int numNodes,
            int numEdges,
            AlgorithmType algorithmType,
            long seed,
            RandomStrategySupport randomSupport
    ) {
        if (numNodes < 2) {
            throw new BadRequestException("numNodes must be at least 2");
        }
        int maxEdges = maxDirectedEdgeCount(numNodes);
        if (numEdges < 0 || numEdges > maxEdges) {
            throw new BadRequestException("numEdges must be between 0 and " + maxEdges + " for directed graph");
        }

        // IMPORTANT: topology RNG must not depend on algorithmType, otherwise algorithm comparisons
        // drift the sampled graph even when using the same seed.
        Random topoRandom = new Random(seed);
        AdvancedAttackGraph graph = new AdvancedAttackGraph();

        for (int id = 1; id <= numNodes; id++) {
            AssetType type = randomAssetType(id, topoRandom);
            double vulnerability = randomSupport.boundedDouble(topoRandom, 0.2, 0.95);
            double defense = randomSupport.boundedDouble(topoRandom, 0.15, 0.85);
            graph.addNode(new AdvancedNode(id, type, vulnerability, defense, false, false, false, null));
        }

        List<int[]> candidates = new ArrayList<>();
        for (int source = 1; source <= numNodes; source++) {
            for (int target = 1; target <= numNodes; target++) {
                if (source == target) {
                    continue;
                }
                candidates.add(new int[]{source, target});
            }
        }
        Collections.shuffle(candidates, topoRandom);

        for (int i = 0; i < numEdges; i++) {
            int[] pair = candidates.get(i);
            double attackCost = randomSupport.boundedDouble(topoRandom, 0.7, 1.7);
            double exploitProbability = randomSupport.boundedDouble(topoRandom, 0.2, 0.92);
            graph.addEdge(pair[0], pair[1], attackCost, exploitProbability);
        }

        ensureGatewayAndDatabase(graph, randomSupport, topoRandom);

        // Security posture is applied after topology is fixed, using a separate RNG stream so
        // algorithm knobs cannot perturb edge sampling.
        long postureSeed = java.util.Objects.hash(seed, "security-posture", String.valueOf(algorithmType)) & Long.MAX_VALUE;
        Random postureRandom = new Random(postureSeed);
        graph.applySecurityPosture(algorithmType, postureRandom);
        return graph;
    }

    /**
     * Models a per-node "encryption posture" used only for compromise scaling in the advanced simulator.
     * This is not a faithful model of link-layer cryptography; it is an explicit modeling assumption.
     */
    public void applySecurityPosture(AlgorithmType algorithmType, Random postureRandom) {
        for (AdvancedNode node : getNodes()) {
            if (node.isHoneypot()) {
                continue;
            }

            if (algorithmType != null) {
                node.setEncryptionModeImpact(algorithmType);
                continue;
            }

            node.setEncryptionModeImpact(randomMode(postureRandom));
        }
    }

    public static int maxDirectedEdgeCount(int numNodes) {
        return numNodes * (numNodes - 1);
    }

    private static AssetType randomAssetType(int nodeId, Random random) {
        if (nodeId == 1) {
            return AssetType.GATEWAY;
        }
        if (nodeId == 2) {
            return AssetType.DATABASE;
        }

        double value = random.nextDouble();
        if (value < 0.35) {
            return AssetType.SERVER;
        }
        if (value < 0.7) {
            return AssetType.IOT_DEVICE;
        }
        return AssetType.GATEWAY;
    }

    private static AlgorithmType randomMode(Random random) {
        AlgorithmType[] values = AlgorithmType.values();
        return values[random.nextInt(values.length)];
    }

    private static void ensureGatewayAndDatabase(AdvancedAttackGraph graph, RandomStrategySupport randomSupport, Random random) {
        List<AdvancedNode> nodes = graph.getNodes();
        if (nodes.stream().noneMatch(node -> node.getAssetType() == AssetType.GATEWAY) && !nodes.isEmpty()) {
            AdvancedNode first = nodes.get(0);
            first.setAssetType(AssetType.GATEWAY);
            first.setDefenseLevel(randomSupport.boundedDouble(random, 0.45, 0.9));
        }
        if (nodes.stream().noneMatch(node -> node.getAssetType() == AssetType.DATABASE) && nodes.size() > 1) {
            AdvancedNode second = nodes.get(1);
            second.setAssetType(AssetType.DATABASE);
            second.setVulnerabilityScore(randomSupport.boundedDouble(random, 0.3, 0.8));
        }
    }

    private boolean wouldCreateFullyOrphanedNetwork() {
        Collection<AdvancedNode> realNodes = nodes.values().stream().filter(node -> !node.isHoneypot()).toList();
        if (realNodes.isEmpty()) {
            return false;
        }

        for (AdvancedNode node : realNodes) {
            int out = enabledOutDegree(node.getId());
            int in = enabledInDegree(node.getId());
            if (out + in > 0) {
                return false;
            }
        }
        return true;
    }

    private Map<Integer, Set<Integer>> buildUndirectedAdjacency(List<Integer> nodeIds) {
        Map<Integer, Set<Integer>> adjacency = new HashMap<>();
        for (Integer nodeId : nodeIds) {
            adjacency.put(nodeId, new HashSet<>());
        }

        for (AdvancedEdge edge : getEnabledEdges()) {
            if (!adjacency.containsKey(edge.getSourceNodeId()) || !adjacency.containsKey(edge.getTargetNodeId())) {
                continue;
            }
            adjacency.get(edge.getSourceNodeId()).add(edge.getTargetNodeId());
            adjacency.get(edge.getTargetNodeId()).add(edge.getSourceNodeId());
        }
        return adjacency;
    }

    private int bfsSize(int startNodeId, Map<Integer, Set<Integer>> adjacency, Set<Integer> visited) {
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(startNodeId);
        visited.add(startNodeId);

        int size = 0;
        while (!queue.isEmpty()) {
            Integer current = queue.poll();
            size++;
            for (Integer neighbor : adjacency.getOrDefault(current, Set.of())) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return size;
    }
}
