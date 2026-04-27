package backend.controller;

import backend.adaptive.ThreatSignalService;
import backend.audit.AuditService;
import backend.dto.ApiSuccessResponse;
import backend.dto.SimulationRunRequest;
import backend.dto.SimulationRunResponse;
import backend.model.AlgorithmType;
import backend.service.AdvancedSimulationService;
import backend.service.EvaluationFrameworkService;
import backend.service.MessageService;
import backend.service.SimulationComparisonService;
import backend.service.SimulationHistoryService;
import backend.simulation.game.GameSimulationService;
import backend.simulation.game.SimulationResult;
import backend.util.RequestContextUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Bridges simulation pressure into the live adaptive threat signal.
 */
class SimulationPressureBridgeTest {

    @Test
    void highAttackBudgetRaisesThreatIntensity() {
        ThreatSignalService threat = new ThreatSignalService();
        threat.setAttackIntensity01(0.20);

        GameSimulationService game = mock(GameSimulationService.class);
        when(game.runSimulation(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SimulationResult(1.0, 0.8, 0.7, 1, 1, 0.3, 0.2, 0.1, AlgorithmType.NORMAL, 0.5));

        SimulationHistoryService history = mock(SimulationHistoryService.class);
        when(history.saveRun(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(null);
        when(history.toRunResponse(null)).thenReturn(new SimulationRunResponse());

        SimulationController controller = new SimulationController(
                game,
                mock(MessageService.class),
                history,
                mock(SimulationComparisonService.class),
                mock(AdvancedSimulationService.class),
                mock(EvaluationFrameworkService.class),
                threat,
                mock(AuditService.class),
                new RequestContextUtil()
        );

        SimulationRunRequest req = new SimulationRunRequest();
        req.setNumNodes(10);
        req.setNumEdges(15);
        req.setAttackBudget(90);
        req.setDefenseBudget(5);
        req.setRecoveryBudget(5);
        req.setAlgorithmType(AlgorithmType.NORMAL);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setRequestURI("/simulation/run");
        TestingAuthenticationToken auth = new TestingAuthenticationToken("alice", "x", "ROLE_SENDER");

        ResponseEntity<ApiSuccessResponse<SimulationRunResponse>> res = controller.run(req, auth, httpReq);
        assertTrue(res.getStatusCode().is2xxSuccessful());
        assertTrue(threat.currentAttackIntensity01() >= 0.80);
    }
}

