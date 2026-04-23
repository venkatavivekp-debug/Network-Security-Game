package backend.service;

import backend.config.AdvancedSimulationProperties;
import backend.dto.EvaluationComparisonResponse;
import backend.dto.EvaluationDefenseCompareMode;
import backend.dto.EvaluationScenarioRequest;
import backend.dto.EvaluationRunResponse;
import backend.model.AlgorithmType;
import backend.model.EvaluationSeedStrategy;
import backend.simulation.advanced.AdaptiveStrategyEngine;
import backend.simulation.advanced.CompromiseProbabilityService;
import backend.simulation.advanced.HoneypotService;
import backend.simulation.advanced.MovingTargetDefenseService;
import backend.simulation.advanced.MultiStageAttackEngine;
import backend.simulation.advanced.RandomStrategySupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationFrameworkServiceTest {

    @Test
    void fixedSeedModeShouldBeDeterministicAcrossRepeatedEvaluations() {
        EvaluationFrameworkService service = buildService();

        EvaluationScenarioRequest request = baseScenario();
        request.setSeedStrategy(EvaluationSeedStrategy.FIXED);
        request.setBaseSeed(42L);
        request.setRepetitions(1);

        EvaluationRunResponse first = service.evaluateWithoutPersistence(request);
        EvaluationRunResponse second = service.evaluateWithoutPersistence(request);

        assertEquals(first.getUsedSeeds(), second.getUsedSeeds());
        assertEquals(first.getAggregateMetrics().getAverageCompromiseRatio(), second.getAggregateMetrics().getAverageCompromiseRatio());
        assertEquals(first.getAggregateMetrics().getAverageResilienceScore(), second.getAggregateMetrics().getAverageResilienceScore());
        assertEquals(first.getAggregateMetrics().getAverageAttackEfficiency(), second.getAggregateMetrics().getAverageAttackEfficiency());
        assertEquals(first.getAggregateMetrics().getStdDevCompromiseRatio(), second.getAggregateMetrics().getStdDevCompromiseRatio());
    }

    @Test
    void variedSeedModeShouldUseDifferentSeedsWhenBaseProvided() {
        EvaluationFrameworkService service = buildService();

        EvaluationScenarioRequest request = baseScenario();
        request.setSeedStrategy(EvaluationSeedStrategy.VARIED);
        request.setBaseSeed(100L);
        request.setRepetitions(4);

        EvaluationRunResponse response = service.evaluateWithoutPersistence(request);

        assertEquals(4, response.getUsedSeeds().size());
        assertEquals(100L, response.getUsedSeeds().get(0));
        assertNotEquals(response.getUsedSeeds().get(0), response.getUsedSeeds().get(1));
        assertNotEquals(response.getUsedSeeds().get(1), response.getUsedSeeds().get(2));
        assertNotEquals(response.getUsedSeeds().get(2), response.getUsedSeeds().get(3));
    }

    @Test
    void securityComparisonShouldReturnThreeAlgorithms() {
        EvaluationFrameworkService service = buildService();

        EvaluationComparisonResponse response = service.compareSecurity(baseScenario());

        assertEquals(3, response.getItems().size());
        assertTrue(response.getItems().stream().anyMatch(item -> item.getAlgorithmType() == AlgorithmType.NORMAL));
        assertTrue(response.getItems().stream().anyMatch(item -> item.getAlgorithmType() == AlgorithmType.SHCS));
        assertTrue(response.getItems().stream().anyMatch(item -> item.getAlgorithmType() == AlgorithmType.CPHS));

        double normalCompromise = response.getItems().stream()
                .filter(item -> item.getAlgorithmType() == AlgorithmType.NORMAL)
                .findFirst()
                .orElseThrow()
                .getAggregateMetrics()
                .getAverageCompromiseRatio();

        double cphsCompromise = response.getItems().stream()
                .filter(item -> item.getAlgorithmType() == AlgorithmType.CPHS)
                .findFirst()
                .orElseThrow()
                .getAggregateMetrics()
                .getAverageCompromiseRatio();

        assertTrue(cphsCompromise <= normalCompromise);
    }

    @Test
    void defenseComparisonMtdShouldProduceOnAndOffItemsWithMetricDifference() {
        EvaluationFrameworkService service = buildService();

        EvaluationComparisonResponse response = service.compareDefense(
                baseScenario(),
                EvaluationDefenseCompareMode.MTD,
                null,
                null,
                null
        );

        assertEquals(2, response.getItems().size());
        assertTrue(response.getItems().stream().anyMatch(item -> "MTD OFF".equals(item.getLabel())));
        assertTrue(response.getItems().stream().anyMatch(item -> "MTD ON".equals(item.getLabel())));

        double offMtd = response.getItems().stream()
                .filter(item -> "MTD OFF".equals(item.getLabel()))
                .findFirst()
                .orElseThrow()
                .getAggregateMetrics()
                .getAverageMtdEffectiveness();

        double onMtd = response.getItems().stream()
                .filter(item -> "MTD ON".equals(item.getLabel()))
                .findFirst()
                .orElseThrow()
                .getAggregateMetrics()
                .getAverageMtdEffectiveness();

        assertTrue(offMtd < onMtd);
    }

    private EvaluationFrameworkService buildService() {
        AdvancedSimulationService advancedSimulationService = buildAdvancedSimulationService();
        return new EvaluationFrameworkService(advancedSimulationService, null, null, new ObjectMapper());
    }

    private AdvancedSimulationService buildAdvancedSimulationService() {
        AdvancedSimulationProperties properties = defaultAdvancedProperties();
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

    private EvaluationScenarioRequest baseScenario() {
        EvaluationScenarioRequest request = new EvaluationScenarioRequest();
        request.setScenarioName("Evaluation Test Scenario");
        request.setNumNodes(20);
        request.setNumEdges(35);
        request.setAttackBudget(6);
        request.setDefenseBudget(6);
        request.setRecoveryBudget(3);
        request.setRounds(10);
        request.setAlgorithmType(AlgorithmType.CPHS);
        request.setEnableMTD(true);
        request.setEnableDeception(true);
        request.setRepetitions(8);
        request.setSeedStrategy(EvaluationSeedStrategy.VARIED);
        request.setBaseSeed(123L);
        return request;
    }

    private AdvancedSimulationProperties defaultAdvancedProperties() {
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
