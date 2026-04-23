package backend.simulation.advanced;

import backend.config.AdvancedSimulationProperties;
import backend.dto.AdvancedSimulationRunRequest;
import backend.dto.AdvancedSimulationRunResponse;
import backend.model.AlgorithmType;
import backend.service.AdvancedSimulationHistoryService;
import backend.service.AdvancedSimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedSimulationEngineTest {

    @Test
    void shouldProduceDeterministicMetricsWithSameSeed() {
        AdvancedSimulationService service = buildService();
        AdvancedSimulationRunRequest request = sampleRequest();

        AdvancedSimulationRunResponse first = service.runWithoutPersistence(request);
        AdvancedSimulationRunResponse second = service.runWithoutPersistence(request);

        assertEquals(first.getSeed(), second.getSeed());
        assertEquals(first.getCompromiseTimeline(), second.getCompromiseTimeline());
        assertEquals(first.getCompromisedNodeCountPerRound(), second.getCompromisedNodeCountPerRound());
        assertEquals(first.getResilienceScore(), second.getResilienceScore());
        assertEquals(first.getAttackEfficiency(), second.getAttackEfficiency());
        assertEquals(first.getDefenseEfficiency(), second.getDefenseEfficiency());
        assertEquals(first.getFinalCompromisedNodes(), second.getFinalCompromisedNodes());
        assertEquals(first.getRoundDetails().size(), second.getRoundDetails().size());
    }

    @Test
    void shouldReturnConsistentResponseShapeForSeededRequests() {
        AdvancedSimulationService service = buildService();
        AdvancedSimulationRunResponse response = service.runWithoutPersistence(sampleRequest());

        assertNotNull(response.getCompromiseTimeline());
        assertNotNull(response.getCompromisedNodeCountPerRound());
        assertNotNull(response.getRoundDetails());
        assertEquals(sampleRequest().getRounds().intValue(), response.getRoundDetails().size());
        assertTrue(response.getSeed() >= 0);
    }

    @Test
    void cphsShouldReduceAttackEfficiencyRelativeToNormalInControlledScenario() {
        AdvancedSimulationService service = buildService();

        AdvancedSimulationRunRequest normalRequest = sampleRequest();
        normalRequest.setAlgorithmType(AlgorithmType.NORMAL);
        AdvancedSimulationRunResponse normal = service.runWithoutPersistence(normalRequest);

        AdvancedSimulationRunRequest cphsRequest = sampleRequest();
        cphsRequest.setAlgorithmType(AlgorithmType.CPHS);
        AdvancedSimulationRunResponse cphs = service.runWithoutPersistence(cphsRequest);

        assertTrue(cphs.getAttackEfficiency() <= normal.getAttackEfficiency());
        assertTrue(cphs.getFinalCompromisedNodes() <= normal.getFinalCompromisedNodes());
    }

    private AdvancedSimulationService buildService() {
        AdvancedSimulationProperties properties = defaultProperties();
        RandomStrategySupport randomSupport = new RandomStrategySupport();
        CompromiseProbabilityService compromiseProbabilityService = new CompromiseProbabilityService(properties, randomSupport);
        MultiStageAttackEngine multiStageAttackEngine = new MultiStageAttackEngine(properties, compromiseProbabilityService);
        MovingTargetDefenseService movingTargetDefenseService = new MovingTargetDefenseService(properties);
        HoneypotService honeypotService = new HoneypotService(properties, randomSupport);

        AdaptiveStrategyEngine adaptiveStrategyEngine = new AdaptiveStrategyEngine(
                properties,
                randomSupport,
                multiStageAttackEngine,
                movingTargetDefenseService,
                honeypotService
        );

        AdvancedSimulationHistoryService historyService = new AdvancedSimulationHistoryService(null, new ObjectMapper());
        return new AdvancedSimulationService(adaptiveStrategyEngine, historyService);
    }

    private AdvancedSimulationRunRequest sampleRequest() {
        AdvancedSimulationRunRequest request = new AdvancedSimulationRunRequest();
        request.setNumNodes(22);
        request.setNumEdges(45);
        request.setAttackBudget(7);
        request.setDefenseBudget(6);
        request.setRecoveryBudget(3);
        request.setRounds(10);
        request.setEnableMTD(true);
        request.setEnableDeception(true);
        request.setAlgorithmType(AlgorithmType.CPHS);
        request.setSeed(42L);
        return request;
    }

    private AdvancedSimulationProperties defaultProperties() {
        AdvancedSimulationProperties properties = new AdvancedSimulationProperties();
        properties.setBaseCompromiseFloor(0.08);
        properties.setVulnerabilityWeight(0.52);
        properties.setExploitWeight(0.38);
        properties.setDefenseMitigationWeight(0.68);
        properties.setPressureWeight(0.35);

        properties.setNormalCompromiseMultiplier(1.0);
        properties.setShcsCompromiseMultiplier(0.84);
        properties.setCphsCompromiseMultiplier(0.68);

        properties.setBaseDetectionProbability(0.12);
        properties.setDefendedDetectionBoost(0.28);
        properties.setHoneypotDetectionBoost(0.44);

        properties.setHardeningNodeCost(1.0);
        properties.setHardeningDefenseBoost(0.12);
        properties.setRecoveryNodeCost(1.0);
        properties.setRecoveryProbabilityBoost(0.34);

        properties.setMtdEdgeShuffleCost(1.0);
        properties.setMtdIdentityRotationCost(1.2);
        properties.setMtdEncryptionRotationCost(1.0);
        properties.setMtdDisablePathCost(0.9);
        properties.setMtdMaxEdgeSwaps(4);
        properties.setMtdMaxPathDisables(4);

        properties.setDeceptionHoneypotCost(1.0);
        properties.setDeceptionMaxHoneypotFraction(0.3);
        properties.setHoneypotLinkProbability(0.45);

        properties.setPersistenceDefenseDecay(0.07);

        properties.setImpactServerWeight(1.0);
        properties.setImpactIotWeight(0.55);
        properties.setImpactGatewayWeight(0.85);
        properties.setImpactDatabaseWeight(1.25);
        properties.setImpactHoneypotWeight(0.2);

        properties.setAttackerPathDepthBenefitWeight(0.08);
        properties.setAttackerDeceptionPenaltyWeight(0.22);

        return properties;
    }
}
