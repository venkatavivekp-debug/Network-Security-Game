package backend.service;

import backend.dto.SimulationComparisonItem;
import backend.dto.SimulationHistoryResponse;
import backend.dto.SimulationRunResponse;
import backend.exception.ResourceNotFoundException;
import backend.model.AlgorithmType;
import backend.model.SimulationRun;
import backend.repository.SimulationRunRepository;
import backend.simulation.game.SimulationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SimulationHistoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationHistoryService.class);

    private final SimulationRunRepository simulationRunRepository;

    public SimulationHistoryService(SimulationRunRepository simulationRunRepository) {
        this.simulationRunRepository = simulationRunRepository;
    }

    @Transactional
    public SimulationRun saveRun(
            int numNodes,
            int numEdges,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget,
            AlgorithmType algorithmType,
            Long messageId,
            SimulationResult result
    ) {
        SimulationRun run = new SimulationRun();
        run.setNumNodes(numNodes);
        run.setNumEdges(numEdges);
        run.setAttackBudget(attackBudget);
        run.setDefenseBudget(defenseBudget);
        run.setRecoveryBudget(recoveryBudget);
        run.setAlgorithmType(algorithmType);
        run.setMessageId(messageId);

        run.setInitialConnectivity(result.getInitialConnectivity());
        run.setAfterAttackConnectivity(result.getAfterAttackConnectivity());
        run.setAfterRecoveryConnectivity(result.getAfterRecoveryConnectivity());
        run.setNodesLost(result.getNodesLost());
        run.setEdgesLost(result.getEdgesLost());
        run.setRecoveryRate(result.getRecoveryRate());
        run.setDefenderUtility(result.getDefenderUtility());
        run.setAttackerUtility(result.getAttackerUtility());
        run.setEffectiveAttackSuccessProbability(result.getEffectiveAttackSuccessProbability());
        run.setCreatedAt(LocalDateTime.now());

        SimulationRun saved = simulationRunRepository.save(run);
        LOGGER.info("Saved simulation run {} for algorithm {}", saved.getId(), saved.getAlgorithmType());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SimulationHistoryResponse> getHistory(AlgorithmType algorithmType) {
        List<SimulationRun> runs = algorithmType == null
                ? simulationRunRepository.findAllByOrderByCreatedAtDesc()
                : simulationRunRepository.findByAlgorithmTypeOrderByCreatedAtDesc(algorithmType);

        return runs.stream().map(this::toHistoryResponse).toList();
    }

    @Transactional(readOnly = true)
    public SimulationHistoryResponse getHistoryById(Long id) {
        SimulationRun run = simulationRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation run not found: " + id));

        return toHistoryResponse(run);
    }

    public SimulationRunResponse toRunResponse(SimulationRun run) {
        SimulationRunResponse response = new SimulationRunResponse();
        response.setSimulationRunId(run.getId());
        response.setInitialConnectivity(run.getInitialConnectivity());
        response.setAfterAttackConnectivity(run.getAfterAttackConnectivity());
        response.setAfterRecoveryConnectivity(run.getAfterRecoveryConnectivity());
        response.setNodesLost(run.getNodesLost());
        response.setEdgesLost(run.getEdgesLost());
        response.setRecoveryRate(run.getRecoveryRate());
        response.setDefenderUtility(run.getDefenderUtility());
        response.setAttackerUtility(run.getAttackerUtility());
        response.setAlgorithmType(run.getAlgorithmType());
        response.setEffectiveAttackSuccessProbability(run.getEffectiveAttackSuccessProbability());
        response.setCreatedAt(run.getCreatedAt());
        return response;
    }

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

    private SimulationHistoryResponse toHistoryResponse(SimulationRun run) {
        SimulationHistoryResponse response = new SimulationHistoryResponse();
        response.setId(run.getId());
        response.setNumNodes(run.getNumNodes());
        response.setNumEdges(run.getNumEdges());
        response.setAttackBudget(run.getAttackBudget());
        response.setDefenseBudget(run.getDefenseBudget());
        response.setRecoveryBudget(run.getRecoveryBudget());
        response.setAlgorithmType(run.getAlgorithmType());
        response.setMessageId(run.getMessageId());

        response.setInitialConnectivity(run.getInitialConnectivity());
        response.setAfterAttackConnectivity(run.getAfterAttackConnectivity());
        response.setAfterRecoveryConnectivity(run.getAfterRecoveryConnectivity());
        response.setNodesLost(run.getNodesLost());
        response.setEdgesLost(run.getEdgesLost());
        response.setRecoveryRate(run.getRecoveryRate());
        response.setDefenderUtility(run.getDefenderUtility());
        response.setAttackerUtility(run.getAttackerUtility());
        response.setEffectiveAttackSuccessProbability(run.getEffectiveAttackSuccessProbability());
        response.setCreatedAt(run.getCreatedAt());
        return response;
    }
}
