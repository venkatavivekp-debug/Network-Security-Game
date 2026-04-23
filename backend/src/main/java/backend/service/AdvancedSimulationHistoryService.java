package backend.service;

import backend.dto.AdvancedRoundDetailResponse;
import backend.dto.AdvancedSimulationRunRequest;
import backend.dto.AdvancedSimulationRunResponse;
import backend.exception.ResourceNotFoundException;
import backend.model.AdvancedSimulationRun;
import backend.model.AlgorithmType;
import backend.repository.AdvancedSimulationRunRepository;
import backend.simulation.advanced.AdvancedSimulationMetrics;
import backend.simulation.advanced.RoundMetrics;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdvancedSimulationHistoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedSimulationHistoryService.class);

    private final AdvancedSimulationRunRepository repository;
    private final ObjectMapper objectMapper;

    public AdvancedSimulationHistoryService(AdvancedSimulationRunRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AdvancedSimulationRun save(
            AdvancedSimulationRunRequest request,
            AdvancedSimulationMetrics metrics,
            long seedUsed
    ) {
        AdvancedSimulationRun run = new AdvancedSimulationRun();
        run.setNumNodes(request.getNumNodes());
        run.setNumEdges(request.getNumEdges());
        run.setAttackBudget(request.getAttackBudget());
        run.setDefenseBudget(request.getDefenseBudget());
        run.setRecoveryBudget(request.getRecoveryBudget());
        run.setRounds(request.getRounds());
        run.setEnableMtd(Boolean.TRUE.equals(request.getEnableMTD()));
        run.setEnableDeception(Boolean.TRUE.equals(request.getEnableDeception()));
        run.setAlgorithmType(request.getAlgorithmType());
        run.setSeedUsed(seedUsed);

        run.setCompromiseTimelineJson(writeJson(metrics.getCompromiseTimeline()));
        run.setCompromisedCountPerRoundJson(writeJson(metrics.getCompromisedNodeCountPerRound()));
        run.setRoundDetailsJson(writeJson(toRoundDetails(metrics.getRoundMetrics())));

        run.setMeanTimeToCompromise(metrics.getMeanTimeToCompromise());
        run.setMaxAttackPathDepth(metrics.getMaxAttackPathDepth());
        run.setResilienceScore(metrics.getResilienceScore());
        run.setDefenseEfficiency(metrics.getDefenseEfficiency());
        run.setAttackEfficiency(metrics.getAttackEfficiency());
        run.setDeceptionEffectiveness(metrics.getDeceptionEffectiveness());
        run.setMtdEffectiveness(metrics.getMtdEffectiveness());
        run.setDetectionRate(metrics.getDetectionRate());
        run.setRecoveryContribution(metrics.getRecoveryContribution());
        run.setFinalCompromisedNodes(metrics.getFinalCompromisedNodes());
        run.setFinalProtectedNodes(metrics.getFinalProtectedNodes());
        run.setAttackerUtility(metrics.getAttackerUtility());
        run.setDefenderUtility(metrics.getDefenderUtility());
        run.setCreatedAt(LocalDateTime.now());

        AdvancedSimulationRun saved = repository.save(run);
        LOGGER.info("Saved advanced simulation run {} for algorithm {}", saved.getId(), saved.getAlgorithmType());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AdvancedSimulationRunResponse> getHistory(AlgorithmType algorithmType) {
        List<AdvancedSimulationRun> runs = algorithmType == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByAlgorithmTypeOrderByCreatedAtDesc(algorithmType);
        return runs.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdvancedSimulationRunResponse getById(Long id) {
        AdvancedSimulationRun run = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advanced simulation run not found: " + id));
        return toResponse(run);
    }

    public AdvancedSimulationRunResponse toResponse(AdvancedSimulationRun run) {
        AdvancedSimulationRunResponse response = new AdvancedSimulationRunResponse();
        response.setAdvancedSimulationRunId(run.getId());
        response.setNumNodes(run.getNumNodes());
        response.setNumEdges(run.getNumEdges());
        response.setAttackBudget(run.getAttackBudget());
        response.setDefenseBudget(run.getDefenseBudget());
        response.setRecoveryBudget(run.getRecoveryBudget());
        response.setRounds(run.getRounds());
        response.setEnableMTD(run.isEnableMtd());
        response.setEnableDeception(run.isEnableDeception());
        response.setAlgorithmType(run.getAlgorithmType());
        response.setSeed(run.getSeedUsed());

        response.setCompromiseTimeline(readDoubleList(run.getCompromiseTimelineJson()));
        response.setCompromisedNodeCountPerRound(readIntegerList(run.getCompromisedCountPerRoundJson()));
        response.setRoundDetails(readRoundDetails(run.getRoundDetailsJson()));

        response.setMeanTimeToCompromise(run.getMeanTimeToCompromise());
        response.setMaxAttackPathDepth(run.getMaxAttackPathDepth());
        response.setResilienceScore(run.getResilienceScore());
        response.setDefenseEfficiency(run.getDefenseEfficiency());
        response.setAttackEfficiency(run.getAttackEfficiency());
        response.setDeceptionEffectiveness(run.getDeceptionEffectiveness());
        response.setMtdEffectiveness(run.getMtdEffectiveness());
        response.setDetectionRate(run.getDetectionRate());
        response.setRecoveryContribution(run.getRecoveryContribution());
        response.setFinalCompromisedNodes(run.getFinalCompromisedNodes());
        response.setFinalProtectedNodes(run.getFinalProtectedNodes());
        response.setAttackerUtility(run.getAttackerUtility());
        response.setDefenderUtility(run.getDefenderUtility());
        response.setCreatedAt(run.getCreatedAt());
        return response;
    }

    public AdvancedSimulationRunResponse toTransientResponse(
            AdvancedSimulationRunRequest request,
            AdvancedSimulationMetrics metrics,
            long seedUsed
    ) {
        AdvancedSimulationRunResponse response = new AdvancedSimulationRunResponse();
        response.setNumNodes(request.getNumNodes());
        response.setNumEdges(request.getNumEdges());
        response.setAttackBudget(request.getAttackBudget());
        response.setDefenseBudget(request.getDefenseBudget());
        response.setRecoveryBudget(request.getRecoveryBudget());
        response.setRounds(request.getRounds());
        response.setEnableMTD(Boolean.TRUE.equals(request.getEnableMTD()));
        response.setEnableDeception(Boolean.TRUE.equals(request.getEnableDeception()));
        response.setAlgorithmType(request.getAlgorithmType());
        response.setSeed(seedUsed);
        response.setCompromiseTimeline(new ArrayList<>(metrics.getCompromiseTimeline()));
        response.setCompromisedNodeCountPerRound(new ArrayList<>(metrics.getCompromisedNodeCountPerRound()));
        response.setRoundDetails(toRoundDetails(metrics.getRoundMetrics()));
        response.setMeanTimeToCompromise(metrics.getMeanTimeToCompromise());
        response.setMaxAttackPathDepth(metrics.getMaxAttackPathDepth());
        response.setResilienceScore(metrics.getResilienceScore());
        response.setDefenseEfficiency(metrics.getDefenseEfficiency());
        response.setAttackEfficiency(metrics.getAttackEfficiency());
        response.setDeceptionEffectiveness(metrics.getDeceptionEffectiveness());
        response.setMtdEffectiveness(metrics.getMtdEffectiveness());
        response.setDetectionRate(metrics.getDetectionRate());
        response.setRecoveryContribution(metrics.getRecoveryContribution());
        response.setFinalCompromisedNodes(metrics.getFinalCompromisedNodes());
        response.setFinalProtectedNodes(metrics.getFinalProtectedNodes());
        response.setAttackerUtility(metrics.getAttackerUtility());
        response.setDefenderUtility(metrics.getDefenderUtility());
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    private List<AdvancedRoundDetailResponse> toRoundDetails(List<RoundMetrics> roundMetrics) {
        List<AdvancedRoundDetailResponse> details = new ArrayList<>();
        for (RoundMetrics metrics : roundMetrics) {
            AdvancedRoundDetailResponse detail = new AdvancedRoundDetailResponse();
            detail.setRoundNumber(metrics.getRoundNumber());
            detail.setTargetedNodes(metrics.getTargetedNodes());
            detail.setNewlyCompromisedNodes(metrics.getNewlyCompromisedNodes());
            detail.setCompromisedNodeCount(metrics.getCompromisedNodeCount());
            detail.setRecoveredNodes(metrics.getRecoveredNodes());
            detail.setDetectedNodes(metrics.getDetectedNodes());
            detail.setHoneypotEngagements(metrics.getHoneypotEngagements());
            detail.setDeceptionSuccessCount(metrics.getDeceptionSuccessCount());
            detail.setMaxAttackPathDepth(metrics.getMaxAttackPathDepth());
            detail.setCompromiseRatio(metrics.getCompromiseRatio());
            detail.setCompromiseImpact(metrics.getCompromiseImpact());
            detail.setAttackerBudgetSpent(metrics.getAttackerBudgetSpent());
            detail.setDefenderBudgetSpent(metrics.getDefenderBudgetSpent());
            detail.setRecoveryCost(metrics.getRecoveryCost());
            detail.setResilienceScore(metrics.getResilienceScore());
            detail.setDefenseEfficiency(metrics.getDefenseEfficiency());
            detail.setAttackEfficiency(metrics.getAttackEfficiency());
            detail.setDeceptionEffectiveness(metrics.getDeceptionEffectiveness());
            detail.setMtdEffectiveness(metrics.getMtdEffectiveness());
            detail.setDetectionRate(metrics.getDetectionRate());
            detail.setAttackerUtility(metrics.getAttackerUtility());
            detail.setDefenderUtility(metrics.getDefenderUtility());
            details.add(detail);
        }
        return details;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize advanced simulation data", ex);
        }
    }

    private List<Double> readDoubleList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse compromise timeline", ex);
        }
    }

    private List<Integer> readIntegerList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse compromised counts", ex);
        }
    }

    private List<AdvancedRoundDetailResponse> readRoundDetails(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<AdvancedRoundDetailResponse>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse round details", ex);
        }
    }
}
