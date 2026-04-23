package backend.controller;

import backend.dto.ApiSuccessResponse;
import backend.dto.EvaluationAggregateMetricsResponse;
import backend.dto.EvaluationRunResponse;
import backend.dto.EvaluationScenarioRequest;
import backend.model.AlgorithmType;
import backend.model.EvaluationComparisonType;
import backend.model.EvaluationSeedStrategy;
import backend.service.EvaluationFrameworkService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationControllerTest {

    @Test
    void evaluateEndpointShouldReturnSuccessEnvelope() {
        SimulationController controller = new SimulationController(
                null,
                null,
                null,
                null,
                null,
                new StubEvaluationFrameworkService()
        );

        EvaluationScenarioRequest request = new EvaluationScenarioRequest();
        request.setScenarioName("Controller Evaluation");
        request.setNumNodes(20);
        request.setNumEdges(35);
        request.setAttackBudget(6);
        request.setDefenseBudget(6);
        request.setRecoveryBudget(3);
        request.setRounds(10);
        request.setAlgorithmType(AlgorithmType.CPHS);
        request.setEnableMTD(true);
        request.setEnableDeception(true);
        request.setRepetitions(5);
        request.setSeedStrategy(EvaluationSeedStrategy.FIXED);
        request.setBaseSeed(42L);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRequestURI("/simulation/evaluate");

        ResponseEntity<ApiSuccessResponse<EvaluationRunResponse>> responseEntity = controller.evaluate(request, httpRequest);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().isSuccess());
        assertEquals("Evaluation run completed", responseEntity.getBody().getMessage());
        assertNotNull(responseEntity.getBody().getData());
        assertEquals(EvaluationComparisonType.SCENARIO, responseEntity.getBody().getData().getComparisonType());
    }

    private static class StubEvaluationFrameworkService extends EvaluationFrameworkService {

        StubEvaluationFrameworkService() {
            super(null, null, null, null);
        }

        @Override
        public EvaluationRunResponse evaluateAndPersist(EvaluationScenarioRequest request) {
            EvaluationAggregateMetricsResponse metrics = new EvaluationAggregateMetricsResponse();
            metrics.setRunsExecuted(request.getRepetitions());
            metrics.setAverageCompromiseRatio(0.2);
            metrics.setAverageResilienceScore(0.8);
            metrics.setAverageAttackEfficiency(0.3);
            metrics.setAverageDefenseEfficiency(0.6);

            EvaluationRunResponse response = new EvaluationRunResponse();
            response.setEvaluationRunId(1L);
            response.setScenarioId(1L);
            response.setComparisonType(EvaluationComparisonType.SCENARIO);
            response.setScenarioName(request.getScenarioName());
            response.setNumNodes(request.getNumNodes());
            response.setNumEdges(request.getNumEdges());
            response.setAttackBudget(request.getAttackBudget());
            response.setDefenseBudget(request.getDefenseBudget());
            response.setRecoveryBudget(request.getRecoveryBudget());
            response.setRounds(request.getRounds());
            response.setAlgorithmType(request.getAlgorithmType());
            response.setEnableMTD(Boolean.TRUE.equals(request.getEnableMTD()));
            response.setEnableDeception(Boolean.TRUE.equals(request.getEnableDeception()));
            response.setRepetitions(request.getRepetitions());
            response.setSeedStrategy(request.getSeedStrategy());
            response.setBaseSeed(request.getBaseSeed());
            response.setUsedSeeds(List.of(42L, 42L, 42L, 42L, 42L));
            response.setAggregateMetrics(metrics);
            response.setCreatedAt(LocalDateTime.now());
            return response;
        }
    }
}
