package backend.service;

import backend.config.GameSimulationProperties;
import backend.dto.SimulationComparisonItem;
import backend.dto.SimulationComparisonResponse;
import backend.model.AlgorithmType;
import backend.simulation.game.GameSimulationService;
import backend.simulation.game.SimulationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationComparisonServiceTest {

    @Test
    void shouldReturnThreeAlgorithmsInComparisonResponse() {
        GameSimulationService gameSimulationService = new GameSimulationService(defaultProperties());
        SimulationHistoryService historyService = new StubHistoryService();
        SimulationComparisonService comparisonService = new SimulationComparisonService(gameSimulationService, historyService);

        SimulationComparisonResponse response = comparisonService.compareAndOptionallyPersist(
                10,
                15,
                3,
                3,
                2,
                false
        );

        assertEquals(3, response.getItems().size());
        List<AlgorithmType> algorithms = response.getItems().stream().map(SimulationComparisonItem::getAlgorithmType).toList();
        assertTrue(algorithms.contains(AlgorithmType.NORMAL));
        assertTrue(algorithms.contains(AlgorithmType.SHCS));
        assertTrue(algorithms.contains(AlgorithmType.CPHS));
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

    private static class StubHistoryService extends SimulationHistoryService {

        StubHistoryService() {
            super(null);
        }

        @Override
        public SimulationComparisonItem toComparisonItem(SimulationResult result) {
            SimulationComparisonItem item = new SimulationComparisonItem();
            item.setAlgorithmType(result.getAlgorithmType());
            item.setInitialConnectivity(result.getInitialConnectivity());
            item.setAfterAttackConnectivity(result.getAfterAttackConnectivity());
            item.setAfterRecoveryConnectivity(result.getAfterRecoveryConnectivity());
            item.setNodesLost(result.getNodesLost());
            item.setEdgesLost(result.getEdgesLost());
            item.setRecoveryRate(result.getRecoveryRate());
            item.setDefenderUtility(result.getDefenderUtility());
            item.setAttackerUtility(result.getAttackerUtility());
            item.setEffectiveAttackSuccessProbability(result.getEffectiveAttackSuccessProbability());
            return item;
        }
    }
}
