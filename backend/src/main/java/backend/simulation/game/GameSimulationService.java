package backend.simulation.game;

import backend.config.GameSimulationProperties;
import backend.exception.BadRequestException;
import backend.model.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Service
public class GameSimulationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameSimulationService.class);

    private final GameSimulationProperties properties;

    public GameSimulationService(GameSimulationProperties properties) {
        this.properties = properties;
    }

    public SimulationResult runSimulation(
            int numNodes,
            int numEdges,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget,
            AlgorithmType algorithmType
    ) {
        return runSimulationInternal(
                numNodes,
                numEdges,
                attackBudget,
                defenseBudget,
                recoveryBudget,
                algorithmType,
                null
        );
    }

    public SimulationResult runSimulationWithSeed(
            int numNodes,
            int numEdges,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget,
            AlgorithmType algorithmType,
            long graphSeed
    ) {
        return runSimulationInternal(
                numNodes,
                numEdges,
                attackBudget,
                defenseBudget,
                recoveryBudget,
                algorithmType,
                graphSeed
        );
    }

    private SimulationResult runSimulationInternal(
            int numNodes,
            int numEdges,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget,
            AlgorithmType algorithmType,
            Long graphSeed
    ) {
        validateInputs(numNodes, numEdges, attackBudget, defenseBudget, recoveryBudget);

        long effectiveSeed = graphSeed != null
                ? graphSeed
                : computeDeterministicSeed(numNodes, numEdges, attackBudget, defenseBudget, recoveryBudget, algorithmType);

        NetworkGraph graph = buildRandomGraph(numNodes, numEdges, new Random(effectiveSeed));
        double initialConnectivity = graph.computeConnectivity();

        DefenseStrategy defenseStrategy = new DefenseStrategy(properties);
        DefenseStrategy.DefenseStageResult defenseResult = defenseStrategy.apply(graph, defenseBudget);

        AlgorithmType effectiveAlgorithmType = resolveSimulationMode(
                algorithmType,
                attackBudget,
                defenseBudget,
                recoveryBudget
        );

        AttackStrategy attackStrategy = new AttackStrategy(properties);
        AttackStrategy.AttackStageResult attackResult = attackStrategy.apply(
                graph,
                attackBudget,
                effectiveAlgorithmType,
                new Random(effectiveSeed ^ 0x9E3779B97F4A7C15L)
        );
        double afterAttackConnectivity = graph.computeConnectivity();

        RecoveryStrategy recoveryStrategy = new RecoveryStrategy(properties);
        RecoveryStrategy.RecoveryStageResult recoveryResult = recoveryStrategy.apply(graph, recoveryBudget);
        double afterRecoveryConnectivity = graph.computeConnectivity();

        double recoveryRate = calculateRecoveryRate(initialConnectivity, afterAttackConnectivity, afterRecoveryConnectivity);
        double defenderUtility = afterRecoveryConnectivity - defenseResult.getDefenseCost() - recoveryResult.getRecoveryCost();

        double damageCaused = calculateDamage(
                initialConnectivity,
                afterAttackConnectivity,
                attackResult.getNodesLost(),
                numNodes,
                attackResult.getEdgesLost(),
                numEdges
        );

        double attackerUtility = damageCaused - attackResult.getAttackCost();

        LOGGER.info(
                "Game simulation complete algo={} effectiveAlgo={} nodes={} edges={} initial={} afterAttack={} afterRecovery={} nodesLost={} edgesLost={}",
                algorithmType,
                effectiveAlgorithmType,
                numNodes,
                numEdges,
                round(initialConnectivity),
                round(afterAttackConnectivity),
                round(afterRecoveryConnectivity),
                attackResult.getNodesLost(),
                attackResult.getEdgesLost()
        );

        return new SimulationResult(
                round(initialConnectivity),
                round(afterAttackConnectivity),
                round(afterRecoveryConnectivity),
                attackResult.getNodesLost(),
                attackResult.getEdgesLost(),
                round(recoveryRate),
                round(defenderUtility),
                round(attackerUtility),
                algorithmType,
                round(attackResult.getEffectiveAttackSuccessProbability())
        );
    }

    private AlgorithmType resolveSimulationMode(
            AlgorithmType algorithmType,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget
    ) {
        if (algorithmType != AlgorithmType.ADAPTIVE) {
            return algorithmType;
        }
        int defenderCapacity = defenseBudget + recoveryBudget;
        if (attackBudget > defenderCapacity) {
            return AlgorithmType.CPHS;
        }
        if (attackBudget > defenseBudget) {
            return AlgorithmType.SHCS;
        }
        return AlgorithmType.NORMAL;
    }

    private void validateInputs(int numNodes, int numEdges, int attackBudget, int defenseBudget, int recoveryBudget) {
        if (numNodes < 2) {
            throw new BadRequestException("numNodes must be at least 2");
        }
        if (numEdges < 0) {
            throw new BadRequestException("numEdges cannot be negative");
        }
        int maxEdges = maxUndirectedEdges(numNodes);
        if (numEdges > maxEdges) {
            throw new BadRequestException("numEdges exceeds maximum possible edges (" + maxEdges + ") for numNodes=" + numNodes);
        }
        if (attackBudget < 0 || defenseBudget < 0 || recoveryBudget < 0) {
            throw new BadRequestException("Budgets cannot be negative");
        }
    }

    private NetworkGraph buildRandomGraph(int numNodes, int numEdges, Random random) {
        NetworkGraph graph = new NetworkGraph();
        for (int nodeId = 1; nodeId <= numNodes; nodeId++) {
            graph.addNode(nodeId);
        }

        if (numEdges == 0) {
            return graph;
        }

        List<int[]> candidateEdges = new ArrayList<>();
        for (int left = 1; left <= numNodes; left++) {
            for (int right = left + 1; right <= numNodes; right++) {
                candidateEdges.add(new int[]{left, right});
            }
        }

        Collections.shuffle(candidateEdges, random);
        for (int i = 0; i < numEdges; i++) {
            int[] edge = candidateEdges.get(i);
            graph.addEdge(edge[0], edge[1]);
        }

        return graph;
    }

    private double calculateDamage(
            double initialConnectivity,
            double afterAttackConnectivity,
            int nodesLost,
            int totalNodes,
            int edgesLost,
            int totalEdges
    ) {
        double connectivityDrop = Math.max(0.0, initialConnectivity - afterAttackConnectivity);
        double nodeLossRatio = totalNodes == 0 ? 0.0 : (double) nodesLost / totalNodes;
        double edgeLossRatio = totalEdges == 0 ? 0.0 : (double) edgesLost / totalEdges;

        return connectivityDrop * properties.getDamageConnectivityWeight()
                + nodeLossRatio * properties.getDamageNodeWeight()
                + edgeLossRatio * properties.getDamageEdgeWeight();
    }

    private double calculateRecoveryRate(
            double initialConnectivity,
            double afterAttackConnectivity,
            double afterRecoveryConnectivity
    ) {
        double preRecoveryDamage = Math.max(0.0, initialConnectivity - afterAttackConnectivity);
        if (preRecoveryDamage == 0.0) {
            return 1.0;
        }

        double postRecoveryDamage = Math.max(0.0, initialConnectivity - afterRecoveryConnectivity);
        double recoveredDamage = preRecoveryDamage - postRecoveryDamage;

        if (recoveredDamage < 0.0) {
            return 0.0;
        }

        return Math.min(1.0, recoveredDamage / preRecoveryDamage);
    }

    private int maxUndirectedEdges(int numNodes) {
        return numNodes * (numNodes - 1) / 2;
    }

    private long computeDeterministicSeed(
            int numNodes,
            int numEdges,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget,
            AlgorithmType algorithmType
    ) {
        return Objects.hash(numNodes, numEdges, attackBudget, defenseBudget, recoveryBudget, algorithmType);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
