package backend.simulation.advanced;

import backend.config.AdvancedSimulationProperties;
import backend.model.AlgorithmType;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovingTargetDefenseServiceTest {

    @Test
    void shouldMutateTopologyWithoutBreakingGraphValidity() {
        AdvancedSimulationProperties properties = defaultProperties();
        RandomStrategySupport randomSupport = new RandomStrategySupport();

        AdvancedAttackGraph graph = AdvancedAttackGraph.generateRandomGraph(
                18,
                40,
                AlgorithmType.NORMAL,
                123L,
                randomSupport
        );

        Set<String> beforeSignature = graph.getEdges().stream()
                .map(edge -> edge.key() + ":" + edge.isEnabled())
                .collect(Collectors.toSet());

        DefenderPolicy policy = new DefenderPolicy();
        policy.setMtdBudget(8.0);

        MovingTargetDefenseService service = new MovingTargetDefenseService(properties);
        MovingTargetDefenseService.MtdActionResult result = service.apply(graph, policy, new Random(55L));

        Set<String> afterSignature = graph.getEdges().stream()
                .map(edge -> edge.key() + ":" + edge.isEnabled())
                .collect(Collectors.toSet());

        assertTrue(graph.isValid());
        assertFalse(graph.getEnabledEdges().isEmpty());
        assertTrue(result.getEffectiveness() >= 0.0 && result.getEffectiveness() <= 1.0);

        boolean topologyChanged = !beforeSignature.equals(afterSignature)
                || result.getEdgeRewirings() > 0
                || result.getPathsDisabled() > 0
                || result.getIdentityRotations() > 0
                || result.getEncryptionRotations() > 0;

        assertTrue(topologyChanged);
    }

    private AdvancedSimulationProperties defaultProperties() {
        AdvancedSimulationProperties properties = new AdvancedSimulationProperties();
        properties.setMtdEdgeShuffleCost(1.0);
        properties.setMtdIdentityRotationCost(1.2);
        properties.setMtdEncryptionRotationCost(1.0);
        properties.setMtdDisablePathCost(0.9);
        properties.setMtdMaxEdgeSwaps(4);
        properties.setMtdMaxPathDisables(4);
        return properties;
    }
}
