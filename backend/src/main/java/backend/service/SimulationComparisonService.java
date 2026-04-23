package backend.service;

import backend.dto.SimulationComparisonItem;
import backend.dto.SimulationComparisonResponse;
import backend.model.AlgorithmType;
import backend.model.SimulationRun;
import backend.simulation.game.GameSimulationService;
import backend.simulation.game.SimulationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SimulationComparisonService {

    private final GameSimulationService gameSimulationService;
    private final SimulationHistoryService simulationHistoryService;

    public SimulationComparisonService(
            GameSimulationService gameSimulationService,
            SimulationHistoryService simulationHistoryService
    ) {
        this.gameSimulationService = gameSimulationService;
        this.simulationHistoryService = simulationHistoryService;
    }

    @Transactional
    public SimulationComparisonResponse compareAndOptionallyPersist(
            int numNodes,
            int numEdges,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget,
            boolean persistRuns
    ) {
        List<SimulationComparisonItem> items = new ArrayList<>();
        long sharedGraphSeed = deriveSharedSeed(numNodes, numEdges, attackBudget, defenseBudget, recoveryBudget);

        for (AlgorithmType algorithmType : List.of(AlgorithmType.NORMAL, AlgorithmType.SHCS, AlgorithmType.CPHS)) {
            SimulationResult result = gameSimulationService.runSimulationWithSeed(
                    numNodes,
                    numEdges,
                    attackBudget,
                    defenseBudget,
                    recoveryBudget,
                    algorithmType,
                    sharedGraphSeed
            );

            if (persistRuns) {
                SimulationRun saved = simulationHistoryService.saveRun(
                        numNodes,
                        numEdges,
                        attackBudget,
                        defenseBudget,
                        recoveryBudget,
                        algorithmType,
                        null,
                        result
                );

                SimulationComparisonItem item = simulationHistoryService.toComparisonItem(result);
                item.setAlgorithmType(saved.getAlgorithmType());
                items.add(item);
            } else {
                items.add(simulationHistoryService.toComparisonItem(result));
            }
        }

        SimulationComparisonResponse response = new SimulationComparisonResponse();
        response.setNumNodes(numNodes);
        response.setNumEdges(numEdges);
        response.setAttackBudget(attackBudget);
        response.setDefenseBudget(defenseBudget);
        response.setRecoveryBudget(recoveryBudget);
        response.setCreatedAt(LocalDateTime.now());
        response.setItems(items);
        return response;
    }

    private long deriveSharedSeed(
            int numNodes,
            int numEdges,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget
    ) {
        return Objects.hash(numNodes, numEdges, attackBudget, defenseBudget, recoveryBudget);
    }
}
