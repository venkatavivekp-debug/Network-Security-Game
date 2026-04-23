package backend.service;

import backend.config.GameSimulationProperties;
import backend.exception.BadRequestException;
import backend.model.AlgorithmType;
import backend.simulation.game.GameSimulationService;
import backend.simulation.game.SimulationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSimulationServiceTest {

    @Test
    void shouldGenerateMetricsWithinExpectedBounds() {
        GameSimulationService simulationService = new GameSimulationService(defaultProperties());

        SimulationResult result = simulationService.runSimulationWithSeed(
                10,
                15,
                3,
                3,
                2,
                AlgorithmType.SHCS,
                12345L
        );

        assertTrue(result.getInitialConnectivity() >= 0.0 && result.getInitialConnectivity() <= 1.0);
        assertTrue(result.getAfterAttackConnectivity() >= 0.0 && result.getAfterAttackConnectivity() <= 1.0);
        assertTrue(result.getAfterRecoveryConnectivity() >= 0.0 && result.getAfterRecoveryConnectivity() <= 1.0);
        assertTrue(result.getRecoveryRate() >= 0.0 && result.getRecoveryRate() <= 1.0);
        assertTrue(result.getNodesLost() >= 0 && result.getNodesLost() <= 10);
        assertTrue(result.getEdgesLost() >= 0 && result.getEdgesLost() <= 15);
        assertTrue(result.getEffectiveAttackSuccessProbability() >= 0.0 && result.getEffectiveAttackSuccessProbability() <= 1.0);
    }

    @Test
    void shouldRejectImpossibleEdgeCount() {
        GameSimulationService simulationService = new GameSimulationService(defaultProperties());

        assertThrows(BadRequestException.class, () ->
                simulationService.runSimulation(6, 100, 1, 1, 1, AlgorithmType.NORMAL)
        );
    }

    private GameSimulationProperties defaultProperties() {
        GameSimulationProperties properties = new GameSimulationProperties();
        properties.setDefenseNodeCost(1.0);
        properties.setDefenseEdgeCost(1.0);
        properties.setAttackNodeCost(1.0);
        properties.setAttackEdgeCost(1.0);
        properties.setRecoveryNodeCost(1.0);
        properties.setRecoveryEdgeCost(1.0);
        properties.setBaseNodeAttackSuccessProbability(0.72);
        properties.setBaseEdgeAttackSuccessProbability(0.68);
        properties.setDefendedTargetSuccessPenalty(0.55);
        properties.setNormalAttackMultiplier(1.0);
        properties.setShcsAttackMultiplier(0.7);
        properties.setCphsAttackMultiplier(0.55);
        properties.setDamageConnectivityWeight(1.0);
        properties.setDamageNodeWeight(0.6);
        properties.setDamageEdgeWeight(0.4);
        return properties;
    }
}
