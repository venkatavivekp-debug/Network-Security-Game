package backend.simulation.advanced;

import backend.config.AdvancedSimulationProperties;
import backend.model.AlgorithmType;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoneypotServiceTest {

    @Test
    void shouldInjectHoneypotsWithValidNodeAttributes() {
        AdvancedSimulationProperties properties = defaultProperties();
        RandomStrategySupport randomSupport = new RandomStrategySupport();
        HoneypotService honeypotService = new HoneypotService(properties, randomSupport);

        AdvancedAttackGraph graph = AdvancedAttackGraph.generateRandomGraph(
                16,
                30,
                AlgorithmType.SHCS,
                77L,
                randomSupport
        );

        int before = graph.countHoneypots();

        DefenderPolicy policy = new DefenderPolicy();
        policy.setDeceptionBudget(6.0);

        DeceptionResult result = honeypotService.deployHoneypots(graph, policy, AlgorithmType.SHCS, new Random(77L));

        int after = graph.countHoneypots();
        assertTrue(after > before);
        assertEquals(result.getHoneypotsInjected(), after - before);

        graph.getNodes().stream()
                .filter(AdvancedNode::isHoneypot)
                .forEach(node -> {
                    assertEquals(AssetType.HONEYPOT, node.getAssetType());
                    assertTrue(node.getDefenseLevel() >= 0.0 && node.getDefenseLevel() <= 1.0);
                });

        assertTrue(result.getDefenseCost() > 0.0);
    }

    private AdvancedSimulationProperties defaultProperties() {
        AdvancedSimulationProperties properties = new AdvancedSimulationProperties();
        properties.setDeceptionHoneypotCost(1.0);
        properties.setDeceptionMaxHoneypotFraction(0.3);
        properties.setHoneypotLinkProbability(0.45);
        return properties;
    }
}
