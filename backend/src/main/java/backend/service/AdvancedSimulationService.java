package backend.service;

import backend.dto.AdvancedSimulationRunRequest;
import backend.dto.AdvancedSimulationRunResponse;
import backend.model.AdvancedSimulationRun;
import backend.model.AlgorithmType;
import backend.simulation.advanced.AdaptiveStrategyEngine;
import backend.simulation.advanced.AdvancedSimulationMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdvancedSimulationService {

    private final AdaptiveStrategyEngine adaptiveStrategyEngine;
    private final AdvancedSimulationHistoryService historyService;

    public AdvancedSimulationService(
            AdaptiveStrategyEngine adaptiveStrategyEngine,
            AdvancedSimulationHistoryService historyService
    ) {
        this.adaptiveStrategyEngine = adaptiveStrategyEngine;
        this.historyService = historyService;
    }

    @Transactional
    public AdvancedSimulationRunResponse runAndPersist(AdvancedSimulationRunRequest request) {
        AdvancedSimulationMetrics metrics = adaptiveStrategyEngine.runSimulation(
                request.getNumNodes(),
                request.getNumEdges(),
                request.getAttackBudget(),
                request.getDefenseBudget(),
                request.getRecoveryBudget(),
                request.getRounds(),
                Boolean.TRUE.equals(request.getEnableMTD()),
                Boolean.TRUE.equals(request.getEnableDeception()),
                request.getAlgorithmType(),
                request.getSeed()
        );

        AdvancedSimulationRun saved = historyService.save(request, metrics, metrics.getSeedUsed());
        return historyService.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AdvancedSimulationRunResponse runWithoutPersistence(AdvancedSimulationRunRequest request) {
        AdvancedSimulationMetrics metrics = adaptiveStrategyEngine.runSimulation(
                request.getNumNodes(),
                request.getNumEdges(),
                request.getAttackBudget(),
                request.getDefenseBudget(),
                request.getRecoveryBudget(),
                request.getRounds(),
                Boolean.TRUE.equals(request.getEnableMTD()),
                Boolean.TRUE.equals(request.getEnableDeception()),
                request.getAlgorithmType(),
                request.getSeed()
        );

        return historyService.toTransientResponse(request, metrics, metrics.getSeedUsed());
    }

    @Transactional(readOnly = true)
    public List<AdvancedSimulationRunResponse> getHistory(AlgorithmType algorithmType) {
        return historyService.getHistory(algorithmType);
    }

    @Transactional(readOnly = true)
    public AdvancedSimulationRunResponse getHistoryById(Long id) {
        return historyService.getById(id);
    }
}
