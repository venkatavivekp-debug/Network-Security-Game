package backend.service;

import backend.adaptive.AdaptiveModePolicyService;
import backend.adaptive.AdaptiveDecision;
import backend.exception.BadRequestException;
import backend.dto.EvaluationModeComparisonSummary;
import backend.model.AlgorithmType;
import backend.model.EvaluationMetrics;
import backend.model.EvaluationResult;
import backend.model.Role;
import backend.model.User;
import backend.repository.EvaluationResultRepository;
import backend.simulation.game.GameSimulationService;
import backend.simulation.game.SimulationResult;
import backend.util.SeedUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class EvaluationExperimentService {

    public enum ExperimentMode {
        NORMAL,
        SHCS,
        CPHS,
        ADAPTIVE
    }

    public enum DefenseStrategy {
        REDUNDANCY,
        DYNAMIC_REROUTING,
        PUZZLE_ESCALATION
    }

    /**
     * Research-style category labels aligned to experiment modes (implementation unchanged; labels only).
     */
    public static String researchCategoryForMode(ExperimentMode mode) {
        return switch (mode) {
            case NORMAL -> "Static Security";
            case SHCS -> "Layered Security";
            case CPHS -> "Challenge-Based Security";
            case ADAPTIVE -> "Adaptive Security";
        };
    }

    public static Map<String, String> baselineModeLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (ExperimentMode mode : ExperimentMode.values()) {
            labels.put(mode.name(), researchCategoryForMode(mode));
        }
        return labels;
    }

    /**
     * Ranks modes using only aggregated metrics: lowest compromise, then highest resilience, then lowest user effort.
     */
    public String bestModeName(Map<String, EvaluationMetrics> metricsByMode) {
        return metricsByMode.entrySet().stream()
                .min(Comparator
                        .comparingDouble((Map.Entry<String, EvaluationMetrics> e) -> e.getValue().getCompromiseRatio())
                        .thenComparingDouble(e -> -e.getValue().getResilienceScore())
                        .thenComparingDouble(e -> e.getValue().getUserEffortScore()))
                .map(Map.Entry::getKey)
                .orElse(ExperimentMode.NORMAL.name());
    }

    public EvaluationModeComparisonSummary buildComparisonSummary(
            double attackIntensity01,
            Map<String, EvaluationMetrics> metricsByMode
    ) {
        EvaluationModeComparisonSummary summary = new EvaluationModeComparisonSummary();
        summary.setAttackIntensity(round3(clamp01(attackIntensity01)));
        summary.setBaselineModeLabels(baselineModeLabels());
        String best = bestModeName(metricsByMode);
        summary.setBestModeUnderAttackIntensity(best);
        summary.setSecurityVsEffortTradeOff(buildSecurityVsEffortSummary(metricsByMode, best, summary.getAttackIntensity()));
        return summary;
    }

    private String buildSecurityVsEffortSummary(
            Map<String, EvaluationMetrics> metricsByMode,
            String bestMode,
            double attackIntensityRounded
    ) {
        List<Map.Entry<String, EvaluationMetrics>> ordered = new ArrayList<>(metricsByMode.entrySet());
        ordered.sort(Comparator.comparingDouble(e -> e.getValue().getCompromiseRatio()));

        StringBuilder sb = new StringBuilder();
        sb.append("At attackIntensity=").append(attackIntensityRounded);
        sb.append(", the lowest measured compromiseRatio is ");
        sb.append(round3(metricsByMode.get(bestMode).getCompromiseRatio()));
        sb.append(" (mode ").append(bestMode).append("). ");
        sb.append("Ordering by compromiseRatio (low to high): ");
        sb.append(ordered.stream()
                .map(e -> e.getKey() + "=" + round3(e.getValue().getCompromiseRatio()))
                .collect(Collectors.joining(", ")));
        sb.append(". ");
        sb.append("Matching userEffortScore values: ");
        sb.append(metricsByMode.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + round3(e.getValue().getUserEffortScore()))
                .collect(Collectors.joining(", ")));
        sb.append(".");
        return sb.toString();
    }

    private final GameSimulationService gameSimulationService;
    private final AdaptiveModePolicyService adaptiveModePolicyService;
    private final EvaluationResultRepository evaluationResultRepository;
    private final ObjectMapper objectMapper;

    public EvaluationExperimentService(
            GameSimulationService gameSimulationService,
            AdaptiveModePolicyService adaptiveModePolicyService,
            EvaluationResultRepository evaluationResultRepository,
            ObjectMapper objectMapper
    ) {
        this.gameSimulationService = gameSimulationService;
        this.adaptiveModePolicyService = adaptiveModePolicyService;
        this.evaluationResultRepository = evaluationResultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, EvaluationMetrics> compare(
            double attackIntensity,
            int numberOfRuns,
            DefenseStrategy defenseStrategy,
            Integer numNodes,
            Integer numEdges,
            Long seed,
            boolean persist
    ) {
        double intensity = clamp01(attackIntensity);
        if (numberOfRuns <= 0 || numberOfRuns > 1000) {
            throw new BadRequestException("numberOfRuns must be between 1 and 1000");
        }
        int nodes = numNodes == null ? 20 : numNodes;
        int edges = numEdges == null ? 35 : numEdges;

        long baseSeed = seed != null ? seed : SeedUtil.stableHash64("evaluation.compare", String.valueOf(nodes), String.valueOf(edges), String.valueOf(intensity), defenseStrategy.name());

        Map<String, EvaluationMetrics> result = new LinkedHashMap<>();
        for (ExperimentMode mode : ExperimentMode.values()) {
            EvaluationMetrics metrics = runExperiment(mode, intensity, numberOfRuns, defenseStrategy, nodes, edges, baseSeed, persist);
            result.put(mode.name(), metrics);
        }
        return result;
    }

    private EvaluationMetrics runExperiment(
            ExperimentMode mode,
            double attackIntensity01,
            int numberOfRuns,
            DefenseStrategy defenseStrategy,
            int numNodes,
            int numEdges,
            long baseSeed,
            boolean persist
    ) {
        double sumAttackSuccess = 0;
        double sumCompromise = 0;
        double sumRecoveryTime = 0;
        double sumResilience = 0;
        double sumEffort = 0;
        int holds = 0;

        for (int i = 0; i < numberOfRuns; i++) {
            long runSeed = SeedUtil.mix64(baseSeed, mode.name(), i);
            RunPlan plan = planRun(mode, defenseStrategy, attackIntensity01, runSeed);

            SimulationResult sim = gameSimulationService.runSimulationWithSeed(
                    numNodes,
                    numEdges,
                    plan.attackBudget,
                    plan.defenseBudget,
                    plan.recoveryBudget,
                    plan.algorithmType,
                    runSeed
            );

            double compromiseRatio = clamp01(1.0 - sim.getAfterAttackConnectivity());
            double recoveryTime = estimateRecoveryTime(sim, plan, attackIntensity01);
            double resilienceScore = computeResilienceScore(compromiseRatio, recoveryTime, sim.getRecoveryRate());

            sumAttackSuccess += sim.getEffectiveAttackSuccessProbability();
            sumCompromise += compromiseRatio;
            sumRecoveryTime += recoveryTime;
            sumResilience += resilienceScore;
            sumEffort += plan.userEffortScore;
            if (plan.communicationHold) {
                holds++;
            }
        }

        EvaluationMetrics metrics = new EvaluationMetrics();
        metrics.setAttackSuccessRate(round3(sumAttackSuccess / numberOfRuns));
        metrics.setCompromiseRatio(round3(sumCompromise / numberOfRuns));
        metrics.setAverageRecoveryTime(round3(sumRecoveryTime / numberOfRuns));
        metrics.setResilienceScore(round3(sumResilience / numberOfRuns));
        metrics.setUserEffortScore(round3(sumEffort / numberOfRuns));
        metrics.setFalsePositiveRate(round3((double) holds / numberOfRuns));

        if (persist) {
            persistResult(mode, baseSeed, attackIntensity01, numberOfRuns, defenseStrategy, numNodes, numEdges, metrics);
        }

        return metrics;
    }

    private RunPlan planRun(ExperimentMode mode, DefenseStrategy defenseStrategy, double attackIntensity01, long seed) {
        // Translate "attackIntensity" + strategy knobs into budgets for the existing game engine.
        int attackBudget = 2 + (int) Math.round(10 * attackIntensity01);
        int defenseBudget = switch (defenseStrategy) {
            case REDUNDANCY -> 6;
            case DYNAMIC_REROUTING -> 5;
            case PUZZLE_ESCALATION -> 4;
        };
        int recoveryBudget = 2 + (int) Math.round(4 * (1.0 - attackIntensity01));

        boolean hold = false;
        AlgorithmType algo;
        double userEffort;

        if (mode == ExperimentMode.ADAPTIVE) {
            // Synthetic sender profile: higher intensity implies higher perceived risk under adversarial pressure.
            User sender = new User("adaptive_sender", "x", Role.SENDER);
            sender.setFailedLoginAttempts((int) Math.round(attackIntensity01 * 6));
            AdaptiveDecision decision = adaptiveModePolicyService.decide(sender, AlgorithmType.NORMAL, "exp", "exp", (int) Math.round(attackIntensity01 * 2), 0);
            algo = decision.getEffectiveMode();
            hold = decision.isCommunicationHold();
            userEffort = estimateUserEffort(decision.getEffectiveMode(), decision.getDifficulty(), attackIntensity01, seed);
        } else {
            algo = AlgorithmType.valueOf(mode.name());
            userEffort = estimateUserEffort(algo, null, attackIntensity01, seed);
        }

        // Defensive concept: puzzle escalation shifts effort from network defense to computation.
        if (defenseStrategy == DefenseStrategy.PUZZLE_ESCALATION && algo == AlgorithmType.CPHS) {
            defenseBudget = Math.max(0, defenseBudget - 1);
            userEffort *= 1.15;
        }

        return new RunPlan(algo, attackBudget, defenseBudget, recoveryBudget, userEffort, hold);
    }

    private double estimateUserEffort(AlgorithmType algorithmType, backend.adaptive.PuzzleDifficulty difficulty, double attackIntensity01, long seed) {
        if (algorithmType == AlgorithmType.NORMAL) return 0.0;
        if (algorithmType == AlgorithmType.SHCS) return 0.15;
        // CPHS: proxy effort based on puzzle size and expected attempts. This stays measurable and reproducible.
        int iters = difficulty == null ? 60_000 : difficulty.getMaxIterations();
        int attempts = difficulty == null ? 3 : difficulty.getAttemptsAllowed();
        Random r = new Random(seed ^ 0xC0FFEE);
        double attemptPressure = 1.0 + (attackIntensity01 * 0.8) + (r.nextDouble() * 0.2);
        return Math.log10(Math.max(10, iters)) * Math.min(3.0, attempts * attemptPressure);
    }

    private double estimateRecoveryTime(SimulationResult sim, RunPlan plan, double attackIntensity01) {
        // Deterministic proxy time: higher loss and lower recovery rate implies longer time to restore.
        double loss = (sim.getNodesLost() * 1.5) + (sim.getEdgesLost() * 0.7);
        double budgetHelp = (plan.recoveryBudget + plan.defenseBudget) * 0.6;
        double ratePenalty = (1.05 - clamp01(sim.getRecoveryRate())) * 8.0;
        double intensityPenalty = attackIntensity01 * 4.0;
        double raw = 5.0 + Math.max(0.0, loss - budgetHelp) * 0.25 + ratePenalty + intensityPenalty;
        return Math.max(1.0, raw);
    }

    private double computeResilienceScore(double compromiseRatio, double recoveryTime, double recoveryRate) {
        // Paper-aligned composite: damage + recovery. Normalized to [0,1].
        double recoveryNorm = 1.0 / (1.0 + (recoveryTime / 20.0));
        double score = (1.0 - compromiseRatio) * 0.55 + clamp01(recoveryRate) * 0.25 + recoveryNorm * 0.20;
        return clamp01(score);
    }

    private void persistResult(
            ExperimentMode mode,
            long baseSeed,
            double attackIntensity01,
            int numberOfRuns,
            DefenseStrategy defenseStrategy,
            int numNodes,
            int numEdges,
            EvaluationMetrics metrics
    ) {
        EvaluationResult row = new EvaluationResult();
        row.setMode(mode.name());
        row.setSeedUsed(baseSeed);
        row.setCreatedAt(LocalDateTime.now());
        row.setParametersJson(toJson(Map.of(
                "attackIntensity", attackIntensity01,
                "numberOfRuns", numberOfRuns,
                "defenseStrategy", defenseStrategy.name(),
                "numNodes", numNodes,
                "numEdges", numEdges
        )));
        row.setMetricsJson(toJson(metrics));
        evaluationResultRepository.save(row);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize evaluation payload", ex);
        }
    }

    private double clamp01(double v) {
        if (!Double.isFinite(v)) return 0;
        return Math.max(0, Math.min(1, v));
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private record RunPlan(
            AlgorithmType algorithmType,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget,
            double userEffortScore,
            boolean communicationHold
    ) {
    }
}

