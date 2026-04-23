package backend.controller;

import backend.dto.AdvancedRoundDetailResponse;
import backend.dto.AdvancedSimulationRunRequest;
import backend.dto.AdvancedSimulationRunResponse;
import backend.dto.ApiSuccessResponse;
import backend.model.AlgorithmType;
import backend.service.AdvancedSimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedSimulationControllerTest {

    @Test
    void advancedRunShouldReturnSuccessEnvelopeWithData() {
        SimulationController controller = new SimulationController(
                null,
                null,
                null,
                null,
                new StubAdvancedSimulationService(),
                null
        );

        AdvancedSimulationRunRequest request = new AdvancedSimulationRunRequest();
        request.setNumNodes(20);
        request.setNumEdges(35);
        request.setAttackBudget(6);
        request.setDefenseBudget(6);
        request.setRecoveryBudget(3);
        request.setRounds(10);
        request.setEnableMTD(true);
        request.setEnableDeception(true);
        request.setAlgorithmType(AlgorithmType.CPHS);
        request.setSeed(42L);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/simulation/advanced-run");

        ResponseEntity<ApiSuccessResponse<AdvancedSimulationRunResponse>> responseEntity =
                controller.advancedRun(request, servletRequest);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().isSuccess());
        assertEquals("Advanced simulation run completed", responseEntity.getBody().getMessage());

        AdvancedSimulationRunResponse data = responseEntity.getBody().getData();
        assertNotNull(data);
        assertNotNull(data.getRoundDetails());
        assertEquals(1, data.getRoundDetails().size());
        assertEquals(42L, data.getSeed());
    }

    private static class StubAdvancedSimulationService extends AdvancedSimulationService {

        StubAdvancedSimulationService() {
            super(null, null);
        }

        @Override
        public AdvancedSimulationRunResponse runAndPersist(AdvancedSimulationRunRequest request) {
            AdvancedRoundDetailResponse roundDetail = new AdvancedRoundDetailResponse();
            roundDetail.setRoundNumber(1);
            roundDetail.setCompromisedNodeCount(2);
            roundDetail.setDetectionRate(0.5);

            AdvancedSimulationRunResponse response = new AdvancedSimulationRunResponse();
            response.setAdvancedSimulationRunId(1L);
            response.setNumNodes(request.getNumNodes());
            response.setNumEdges(request.getNumEdges());
            response.setAttackBudget(request.getAttackBudget());
            response.setDefenseBudget(request.getDefenseBudget());
            response.setRecoveryBudget(request.getRecoveryBudget());
            response.setRounds(request.getRounds());
            response.setEnableMTD(Boolean.TRUE.equals(request.getEnableMTD()));
            response.setEnableDeception(Boolean.TRUE.equals(request.getEnableDeception()));
            response.setAlgorithmType(request.getAlgorithmType());
            response.setSeed(request.getSeed() == null ? 0L : request.getSeed());
            response.setCompromiseTimeline(List.of(0.1));
            response.setCompromisedNodeCountPerRound(List.of(2));
            response.setRoundDetails(List.of(roundDetail));
            response.setResilienceScore(0.8);
            response.setAttackEfficiency(0.4);
            response.setDefenseEfficiency(0.6);
            response.setCreatedAt(LocalDateTime.now());
            return response;
        }
    }
}
