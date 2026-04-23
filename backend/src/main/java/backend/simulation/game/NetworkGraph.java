package backend.simulation.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class NetworkGraph {

    private final Map<Integer, NetworkNode> nodes = new HashMap<>();
    private final Map<String, NetworkEdge> edges = new HashMap<>();

    public void addNode(int nodeId) {
        nodes.putIfAbsent(nodeId, new NetworkNode(nodeId));
    }

    public void addEdge(int fromNodeId, int toNodeId) {
        if (fromNodeId == toNodeId) {
            return;
        }
        addNode(fromNodeId);
        addNode(toNodeId);
        NetworkEdge edge = new NetworkEdge(fromNodeId, toNodeId);
        edges.putIfAbsent(edge.key(), edge);
    }

    public Collection<NetworkNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Collection<NetworkEdge> getEdges() {
        return Collections.unmodifiableCollection(edges.values());
    }

    public NetworkNode getNode(int nodeId) {
        return nodes.get(nodeId);
    }

    public int totalNodeCount() {
        return nodes.size();
    }

    public int totalEdgeCount() {
        return edges.size();
    }

    public boolean containsEdge(int left, int right) {
        return edges.containsKey(edgeKey(left, right));
    }

    public int degreeOf(int nodeId, boolean activeOnly) {
        NetworkNode node = nodes.get(nodeId);
        if (node == null || (activeOnly && !node.isActive())) {
            return 0;
        }

        int degree = 0;
        for (NetworkEdge edge : edges.values()) {
            if (activeOnly && !edge.isActive()) {
                continue;
            }

            int from = edge.getFromNodeId();
            int to = edge.getToNodeId();
            if ((from == nodeId && isEndpointUsable(to, activeOnly)) || (to == nodeId && isEndpointUsable(from, activeOnly))) {
                degree++;
            }
        }
        return degree;
    }

    public List<Integer> activeNeighborsOf(int nodeId) {
        List<Integer> neighbors = new ArrayList<>();
        if (!nodes.containsKey(nodeId) || !nodes.get(nodeId).isActive()) {
            return neighbors;
        }

        for (NetworkEdge edge : edges.values()) {
            if (!edge.isActive()) {
                continue;
            }
            int from = edge.getFromNodeId();
            int to = edge.getToNodeId();
            if (from == nodeId && isNodeActive(to)) {
                neighbors.add(to);
            } else if (to == nodeId && isNodeActive(from)) {
                neighbors.add(from);
            }
        }
        return neighbors;
    }

    public boolean disableNode(int nodeId) {
        NetworkNode node = nodes.get(nodeId);
        if (node == null || !node.isActive()) {
            return false;
        }

        node.setActive(false);
        node.setCompromised(true);

        for (NetworkEdge edge : edges.values()) {
            if (edge.getFromNodeId() == nodeId || edge.getToNodeId() == nodeId) {
                edge.setActive(false);
                edge.setCompromised(true);
            }
        }
        return true;
    }

    public boolean disableEdge(int left, int right) {
        NetworkEdge edge = edges.get(edgeKey(left, right));
        if (edge == null || !edge.isActive()) {
            return false;
        }

        edge.setActive(false);
        edge.setCompromised(true);
        return true;
    }

    public boolean recoverNode(int nodeId) {
        NetworkNode node = nodes.get(nodeId);
        if (node == null || node.isActive()) {
            return false;
        }

        node.setActive(true);
        node.setCompromised(false);
        return true;
    }

    public boolean recoverEdge(int left, int right) {
        NetworkEdge edge = edges.get(edgeKey(left, right));
        if (edge == null || edge.isActive()) {
            return false;
        }

        if (!isNodeActive(edge.getFromNodeId()) || !isNodeActive(edge.getToNodeId())) {
            return false;
        }

        edge.setActive(true);
        edge.setCompromised(false);
        return true;
    }

    public int countCompromisedNodes() {
        int count = 0;
        for (NetworkNode node : nodes.values()) {
            if (!node.isActive()) {
                count++;
            }
        }
        return count;
    }

    public int countCompromisedEdges() {
        int count = 0;
        for (NetworkEdge edge : edges.values()) {
            if (!edge.isActive()) {
                count++;
            }
        }
        return count;
    }

    public double computeConnectivity() {
        List<Integer> activeNodeIds = nodes.values().stream()
                .filter(NetworkNode::isActive)
                .map(NetworkNode::getId)
                .sorted()
                .toList();

        int n = activeNodeIds.size();
        if (n <= 1) {
            return 1.0;
        }

        long totalPairs = (long) n * (n - 1) / 2;
        long connectedPairs = 0;

        Set<Integer> visited = new HashSet<>();
        for (Integer nodeId : activeNodeIds) {
            if (visited.contains(nodeId)) {
                continue;
            }

            int componentSize = bfsComponentSize(nodeId, visited);
            connectedPairs += (long) componentSize * (componentSize - 1) / 2;
        }

        return (double) connectedPairs / totalPairs;
    }

    private int bfsComponentSize(int startNodeId, Set<Integer> visited) {
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(startNodeId);
        visited.add(startNodeId);

        int size = 0;
        while (!queue.isEmpty()) {
            int current = queue.poll();
            size++;

            for (int neighbor : activeNeighborsOf(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return size;
    }

    private boolean isEndpointUsable(int nodeId, boolean activeOnly) {
        if (!nodes.containsKey(nodeId)) {
            return false;
        }
        return !activeOnly || nodes.get(nodeId).isActive();
    }

    private boolean isNodeActive(int nodeId) {
        NetworkNode node = nodes.get(nodeId);
        return node != null && node.isActive();
    }

    private String edgeKey(int left, int right) {
        int a = Math.min(left, right);
        int b = Math.max(left, right);
        return a + "-" + b;
    }
}
