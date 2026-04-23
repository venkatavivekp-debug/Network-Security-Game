package backend.service;

import backend.dto.AdvancedSimulationRunRequest;
import backend.dto.AdvancedSimulationRunResponse;
import backend.dto.EvaluationAggregateMetricsResponse;
import backend.dto.EvaluationComparisonItem;
import backend.dto.EvaluationComparisonResponse;
import backend.dto.EvaluationDefenseCompareMode;
import backend.dto.EvaluationScenarioRequest;
import backend.dto.EvaluationRunResponse;
import backend.exception.BadRequestException;
import backend.exception.ResourceNotFoundException;
import backend.model.AlgorithmType;
import backend.model.EvaluationComparisonType;
import backend.model.EvaluationRun;
import backend.model.EvaluationScenario;
import backend.model.EvaluationSeedStrategy;
import backend.repository.EvaluationRunRepository;
import backend.repository.EvaluationScenarioRepository;
import backend.util.SeedUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationFrameworkService {

    private final AdvancedSimulationService advancedSimulationService;
    private final EvaluationScenarioRepository evaluationScenarioRepository;
    private final EvaluationRunRepository evaluationRunRepository;
    private final ObjectMapper objectMapper;

    public EvaluationFrameworkService(
            AdvancedSimulationService advancedSimulationService,
            EvaluationScenarioRepository evaluationScenarioRepository,
            EvaluationRunRepository evaluationRunRepository,
            ObjectMapper objectMapper
    ) {
        this.advancedSimulationService = advancedSimulationService;
        this.evaluationScenarioRepository = evaluationScenarioRepository;
        this.evaluationRunRepository = evaluationRunRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EvaluationRunResponse evaluateAndPersist(EvaluationScenarioRequest request) {
        ScenarioExecution execution = executeScenario(request);

        EvaluationScenario scenario = new EvaluationScenario();
        scenario.setScenarioName(request.getScenarioName());
        scenario.setNumNodes(request.getNumNodes());
        scenario.setNumEdges(request.getNumEdges());
        scenario.setAttackBudget(request.getAttackBudget());
        scenario.setDefenseBudget(request.getDefenseBudget());
        scenario.setRecoveryBudget(request.getRecoveryBudget());
        scenario.setRounds(request.getRounds());
        scenario.setAlgorithmType(request.getAlgorithmType());
        scenario.setEnableMtd(Boolean.TRUE.equals(request.getEnableMTD()));
        scenario.setEnableDeception(Boolean.TRUE.equals(request.getEnableDeception()));
        scenario.setRepetitions(request.getRepetitions());
        scenario.setSeedStrategy(request.getSeedStrategy());
        scenario.setBaseSeed(request.getBaseSeed());
        scenario.setCreatedAt(LocalDateTime.now());

        EvaluationScenario savedScenario = evaluationScenarioRepository.save(scenario);

        EvaluationRun run = buildEvaluationRun(
                savedScenario,
                request,
                EvaluationComparisonType.SCENARIO,
                execution.aggregateMetrics,
                execution.usedSeeds,
                null
        );

        EvaluationRun savedRun = evaluationRunRepository.save(run);
        return toResponse(savedRun);
    }

    @Transactional(readOnly = true)
    public EvaluationRunResponse evaluateWithoutPersistence(EvaluationScenarioRequest request) {
        ScenarioExecution execution = executeScenario(request);
        EvaluationRun transientRun = buildEvaluationRun(
                null,
                request,
                EvaluationComparisonType.SCENARIO,
                execution.aggregateMetrics,
                execution.usedSeeds,
                null
        );
        transientRun.setCreatedAt(LocalDateTime.now());
        return toResponse(transientRun);
    }

    @Transactional(readOnly = true)
    public List<EvaluationRunResponse> getEvaluations(AlgorithmType algorithmType, EvaluationComparisonType comparisonType) {
        List<EvaluationRun> runs;

        if (algorithmType != null && comparisonType != null) {
            runs = evaluationRunRepository.findByAlgorithmTypeAndComparisonTypeOrderByCreatedAtDesc(algorithmType, comparisonType);
        } else if (algorithmType != null) {
            runs = evaluationRunRepository.findByAlgorithmTypeOrderByCreatedAtDesc(algorithmType);
        } else if (comparisonType != null) {
            runs = evaluationRunRepository.findByComparisonTypeOrderByCreatedAtDesc(comparisonType);
        } else {
            runs = evaluationRunRepository.findAllByOrderByCreatedAtDesc();
        }

        return runs.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EvaluationRunResponse getEvaluationById(Long id) {
        EvaluationRun run = evaluationRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation run not found: " + id));
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public EvaluationComparisonResponse compareSecurity(EvaluationScenarioRequest baseScenario) {
        EvaluationComparisonResponse response = new EvaluationComparisonResponse();
        response.setComparisonType(EvaluationComparisonType.SECURITY_ALGORITHM);
        response.setScenarioName(baseScenario.getScenarioName());
        response.setNumNodes(baseScenario.getNumNodes());
        response.setNumEdges(baseScenario.getNumEdges());
        response.setRounds(baseScenario.getRounds());
        response.setRepetitions(baseScenario.getRepetitions());

        List<EvaluationComparisonItem> items = new ArrayList<>();
        for (AlgorithmType algorithmType : List.of(AlgorithmType.NORMAL, AlgorithmType.SHCS, AlgorithmType.CPHS)) {
            EvaluationScenarioRequest scenario = copyScenario(baseScenario);
            scenario.setAlgorithmType(algorithmType);
            ScenarioExecution execution = executeScenario(scenario);

            EvaluationComparisonItem item = new EvaluationComparisonItem();
            item.setLabel(String.valueOf(algorithmType));
            item.setAlgorithmType(algorithmType);
            item.setEnableMTD(Boolean.TRUE.equals(scenario.getEnableMTD()));
            item.setEnableDeception(Boolean.TRUE.equals(scenario.getEnableDeception()));
            item.setAttackBudget(scenario.getAttackBudget());
            item.setDefenseBudget(scenario.getDefenseBudget());
            item.setAggregateMetrics(execution.aggregateMetrics);
            items.add(item);
        }

        response.setItems(items);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public EvaluationComparisonResponse compareDefense(
            EvaluationScenarioRequest baseScenario,
            EvaluationDefenseCompareMode mode,
            Integer sweepStart,
            Integer sweepEnd,
            Integer sweepStep
    ) {
        EvaluationComparisonResponse response = new EvaluationComparisonResponse();
        response.setScenarioName(baseScenario.getScenarioName());
        response.setNumNodes(baseScenario.getNumNodes());
        response.setNumEdges(baseScenario.getNumEdges());
        response.setRounds(baseScenario.getRounds());
        response.setRepetitions(baseScenario.getRepetitions());

        List<EvaluationComparisonItem> items = new ArrayList<>();

        switch (mode) {
            case MTD -> {
                response.setComparisonType(EvaluationComparisonType.DEFENSE_MTD);
                items.add(runDefenseItem(baseScenario, false, Boolean.TRUE.equals(baseScenario.getEnableDeception()), "MTD OFF"));
                items.add(runDefenseItem(baseScenario, true, Boolean.TRUE.equals(baseScenario.getEnableDeception()), "MTD ON"));
            }
            case DECEPTION -> {
                response.setComparisonType(EvaluationComparisonType.DEFENSE_DECEPTION);
                items.add(runDefenseItem(baseScenario, Boolean.TRUE.equals(baseScenario.getEnableMTD()), false, "Deception OFF"));
                items.add(runDefenseItem(baseScenario, Boolean.TRUE.equals(baseScenario.getEnableMTD()), true, "Deception ON"));
            }
            case ATTACK_BUDGET -> {
                response.setComparisonType(EvaluationComparisonType.ATTACK_BUDGET_SWEEP);
                addAttackBudgetSweepItems(baseScenario, sweepStart, sweepEnd, sweepStep, items);
            }
            case DEFENSE_BUDGET -> {
                response.setComparisonType(EvaluationComparisonType.DEFENSE_BUDGET_SWEEP);
                addDefenseBudgetSweepItems(baseScenario, sweepStart, sweepEnd, sweepStep, items);
            }
            default -> throw new BadRequestException("Unsupported defense comparison mode: " + mode);
        }

        response.setItems(items);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public byte[] exportEvaluationsCsv(AlgorithmType algorithmType, EvaluationComparisonType comparisonType) {
        List<EvaluationRunResponse> runs = getEvaluations(algorithmType, comparisonType);

        StringBuilder builder = new StringBuilder();
        builder.append("evaluationRunId,scenarioId,comparisonType,scenarioName,algorithmType,enableMTD,enableDeception,repetitions,seedStrategy,baseSeed,")
                .append("numNodes,numEdges,attackBudget,defenseBudget,recoveryBudget,rounds,")
                .append("avgFinalCompromisedNodes,avgCompromiseRatio,avgResilienceScore,avgAttackEfficiency,avgDefenseEfficiency,")
                .append("avgDeceptionEffectiveness,avgMtdEffectiveness,avgMeanTimeToCompromise,avgAttackPathDepth,")
                .append("stdFinalCompromisedNodes,stdCompromiseRatio,stdResilienceScore,stdAttackEfficiency,stdDefenseEfficiency,stdMeanTimeToCompromise,")
                .append("createdAt")
                .append("\n");

        for (EvaluationRunResponse run : runs) {
            EvaluationAggregateMetricsResponse metrics = run.getAggregateMetrics();
            builder.append(csv(run.getEvaluationRunId())).append(',')
                    .append(csv(run.getScenarioId())).append(',')
                    .append(csv(run.getComparisonType())).append(',')
                    .append(csv(run.getScenarioName())).append(',')
                    .append(csv(run.getAlgorithmType())).append(',')
                    .append(csv(run.isEnableMTD())).append(',')
                    .append(csv(run.isEnableDeception())).append(',')
                    .append(csv(run.getRepetitions())).append(',')
                    .append(csv(run.getSeedStrategy())).append(',')
                    .append(csv(run.getBaseSeed())).append(',')
                    .append(csv(run.getNumNodes())).append(',')
                    .append(csv(run.getNumEdges())).append(',')
                    .append(csv(run.getAttackBudget())).append(',')
                    .append(csv(run.getDefenseBudget())).append(',')
                    .append(csv(run.getRecoveryBudget())).append(',')
                    .append(csv(run.getRounds())).append(',')
                    .append(csv(metrics.getAverageFinalCompromisedNodes())).append(',')
                    .append(csv(metrics.getAverageCompromiseRatio())).append(',')
                    .append(csv(metrics.getAverageResilienceScore())).append(',')
                    .append(csv(metrics.getAverageAttackEfficiency())).append(',')
                    .append(csv(metrics.getAverageDefenseEfficiency())).append(',')
                    .append(csv(metrics.getAverageDeceptionEffectiveness())).append(',')
                    .append(csv(metrics.getAverageMtdEffectiveness())).append(',')
                    .append(csv(metrics.getAverageMeanTimeToCompromise())).append(',')
                    .append(csv(metrics.getAverageAttackPathDepth())).append(',')
                    .append(csv(metrics.getStdDevFinalCompromisedNodes())).append(',')
                    .append(csv(metrics.getStdDevCompromiseRatio())).append(',')
                    .append(csv(metrics.getStdDevResilienceScore())).append(',')
                    .append(csv(metrics.getStdDevAttackEfficiency())).append(',')
                    .append(csv(metrics.getStdDevDefenseEfficiency())).append(',')
                    .append(csv(metrics.getStdDevMeanTimeToCompromise())).append(',')
                    .append(csv(run.getCreatedAt()))
                    .append('\n');
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private EvaluationComparisonItem runDefenseItem(
            EvaluationScenarioRequest baseScenario,
            boolean enableMtd,
            boolean enableDeception,
            String label
    ) {
        EvaluationScenarioRequest scenario = copyScenario(baseScenario);
        scenario.setEnableMTD(enableMtd);
        scenario.setEnableDeception(enableDeception);
        ScenarioExecution execution = executeScenario(scenario);

        EvaluationComparisonItem item = new EvaluationComparisonItem();
        item.setLabel(label);
        item.setAlgorithmType(scenario.getAlgorithmType());
        item.setEnableMTD(enableMtd);
        item.setEnableDeception(enableDeception);
        item.setAttackBudget(scenario.getAttackBudget());
        item.setDefenseBudget(scenario.getDefenseBudget());
        item.setAggregateMetrics(execution.aggregateMetrics);
        return item;
    }

    private void addAttackBudgetSweepItems(
            EvaluationScenarioRequest baseScenario,
            Integer sweepStart,
            Integer sweepEnd,
            Integer sweepStep,
            List<EvaluationComparisonItem> items
    ) {
        int start = validateSweepStart(sweepStart);
        int end = validateSweepEnd(sweepEnd);
        int step = validateSweepStep(sweepStep);
        if (end < start) {
            throw new BadRequestException("sweepEnd must be greater than or equal to sweepStart");
        }

        for (int budget = start; budget <= end; budget += step) {
            EvaluationScenarioRequest scenario = copyScenario(baseScenario);
            scenario.setAttackBudget(budget);

            ScenarioExecution execution = executeScenario(scenario);
            EvaluationComparisonItem item = new EvaluationComparisonItem();
            item.setLabel("Attack Budget " + budget);
            item.setAlgorithmType(scenario.getAlgorithmType());
            item.setEnableMTD(Boolean.TRUE.equals(scenario.getEnableMTD()));
            item.setEnableDeception(Boolean.TRUE.equals(scenario.getEnableDeception()));
            item.setAttackBudget(budget);
            item.setDefenseBudget(scenario.getDefenseBudget());
            item.setAggregateMetrics(execution.aggregateMetrics);
            items.add(item);
        }
    }

    private void addDefenseBudgetSweepItems(
            EvaluationScenarioRequest baseScenario,
            Integer sweepStart,
            Integer sweepEnd,
            Integer sweepStep,
            List<EvaluationComparisonItem> items
    ) {
        int start = validateSweepStart(sweepStart);
        int end = validateSweepEnd(sweepEnd);
        int step = validateSweepStep(sweepStep);
        if (end < start) {
            throw new BadRequestException("sweepEnd must be greater than or equal to sweepStart");
        }

        for (int budget = start; budget <= end; budget += step) {
            EvaluationScenarioRequest scenario = copyScenario(baseScenario);
            scenario.setDefenseBudget(budget);

            ScenarioExecution execution = executeScenario(scenario);
            EvaluationComparisonItem item = new EvaluationComparisonItem();
            item.setLabel("Defense Budget " + budget);
            item.setAlgorithmType(scenario.getAlgorithmType());
            item.setEnableMTD(Boolean.TRUE.equals(scenario.getEnableMTD()));
            item.setEnableDeception(Boolean.TRUE.equals(scenario.getEnableDeception()));
            item.setAttackBudget(scenario.getAttackBudget());
            item.setDefenseBudget(budget);
            item.setAggregateMetrics(execution.aggregateMetrics);
            items.add(item);
        }
    }

    private int validateSweepStart(Integer value) {
        if (value == null || value < 0) {
            throw new BadRequestException("sweepStart must be provided and non-negative");
        }
        return value;
    }

    private int validateSweepEnd(Integer value) {
        if (value == null || value < 0) {
            throw new BadRequestException("sweepEnd must be provided and non-negative");
        }
        return value;
    }

    private int validateSweepStep(Integer value) {
        if (value == null || value < 1) {
            throw new BadRequestException("sweepStep must be at least 1");
        }
        return value;
    }

    private ScenarioExecution executeScenario(EvaluationScenarioRequest request) {
        validateScenarioRequest(request);
        List<AdvancedSimulationRunResponse> runResults = new ArrayList<>();
        List<Long> usedSeeds = new ArrayList<>();

        for (int i = 0; i < request.getRepetitions(); i++) {
            long seed = resolveSeed(request, i);
            usedSeeds.add(seed);

            AdvancedSimulationRunRequest advancedRequest = new AdvancedSimulationRunRequest();
            advancedRequest.setNumNodes(request.getNumNodes());
            advancedRequest.setNumEdges(request.getNumEdges());
            advancedRequest.setAttackBudget(request.getAttackBudget());
            advancedRequest.setDefenseBudget(request.getDefenseBudget());
            advancedRequest.setRecoveryBudget(request.getRecoveryBudget());
            advancedRequest.setRounds(request.getRounds());
            advancedRequest.setAlgorithmType(request.getAlgorithmType());
            advancedRequest.setEnableMTD(request.getEnableMTD());
            advancedRequest.setEnableDeception(request.getEnableDeception());
            advancedRequest.setSeed(seed);

            AdvancedSimulationRunResponse result = advancedSimulationService.runWithoutPersistence(advancedRequest);
            runResults.add(result);
        }

        EvaluationAggregateMetricsResponse aggregateMetrics = aggregate(runResults, request.getNumNodes());
        return new ScenarioExecution(aggregateMetrics, usedSeeds);
    }

    private void validateScenarioRequest(EvaluationScenarioRequest request) {
        if (request == null) {
            throw new BadRequestException("Evaluation scenario request is required");
        }
        if (request.getScenarioName() == null || request.getScenarioName().isBlank()) {
            throw new BadRequestException("scenarioName is required");
        }
        if (request.getNumNodes() == null || request.getNumNodes() < 2) {
            throw new BadRequestException("numNodes must be at least 2");
        }
        if (request.getNumEdges() == null || request.getNumEdges() < 0) {
            throw new BadRequestException("numEdges cannot be negative");
        }
        int maxDirectedEdges = request.getNumNodes() * (request.getNumNodes() - 1);
        if (request.getNumEdges() > maxDirectedEdges) {
            throw new BadRequestException("numEdges exceeds maximum possible directed edges for numNodes");
        }
        if (request.getAttackBudget() == null || request.getAttackBudget() < 0
                || request.getDefenseBudget() == null || request.getDefenseBudget() < 0
                || request.getRecoveryBudget() == null || request.getRecoveryBudget() < 0) {
            throw new BadRequestException("Budgets cannot be negative");
        }
        if (request.getRounds() == null || request.getRounds() < 1) {
            throw new BadRequestException("rounds must be at least 1");
        }
        if (request.getRepetitions() == null || request.getRepetitions() < 1) {
            throw new BadRequestException("repetitions must be at least 1");
        }
        if (request.getAlgorithmType() == null) {
            throw new BadRequestException("algorithmType is required");
        }
        if (request.getSeedStrategy() == null) {
            throw new BadRequestException("seedStrategy is required");
        }
        if (request.getBaseSeed() != null && request.getBaseSeed() < 0) {
            throw new BadRequestException("baseSeed must be non-negative");
        }

        if (request.getSeedStrategy() == EvaluationSeedStrategy.FIXED && request.getRepetitions() != null && request.getRepetitions() > 1) {
            throw new BadRequestException(
                    "seedStrategy=FIXED runs a single deterministic trajectory; repetitions must be 1 (use VARIED for repeated draws)"
            );
        }
        if (request.getSeedStrategy() == EvaluationSeedStrategy.FIXED && request.getBaseSeed() == null) {
            throw new BadRequestException("seedStrategy=FIXED requires baseSeed");
        }
    }

    private EvaluationAggregateMetricsResponse aggregate(List<AdvancedSimulationRunResponse> runs, int totalNodes) {
        if (runs.isEmpty()) {
            throw new BadRequestException("No runs executed for evaluation");
        }

        StatAccumulator finalCompromised = new StatAccumulator();
        StatAccumulator compromiseRatio = new StatAccumulator();
        StatAccumulator resilience = new StatAccumulator();
        StatAccumulator attackEfficiency = new StatAccumulator();
        StatAccumulator defenseEfficiency = new StatAccumulator();
        StatAccumulator deceptionEffectiveness = new StatAccumulator();
        StatAccumulator mtdEffectiveness = new StatAccumulator();
        StatAccumulator meanTimeToCompromise = new StatAccumulator();
        StatAccumulator pathDepth = new StatAccumulator();

        for (AdvancedSimulationRunResponse run : runs) {
            finalCompromised.add(run.getFinalCompromisedNodes());
            double ratio = totalNodes == 0 ? 0.0 : (double) run.getFinalCompromisedNodes() / totalNodes;
            compromiseRatio.add(ratio);
            resilience.add(run.getResilienceScore());
            attackEfficiency.add(run.getAttackEfficiency());
            defenseEfficiency.add(run.getDefenseEfficiency());
            deceptionEffectiveness.add(run.getDeceptionEffectiveness());
            mtdEffectiveness.add(run.getMtdEffectiveness());
            meanTimeToCompromise.add(run.getMeanTimeToCompromise());
            pathDepth.add(run.getMaxAttackPathDepth());
        }

        EvaluationAggregateMetricsResponse metrics = new EvaluationAggregateMetricsResponse();
        metrics.setRunsExecuted(runs.size());

        metrics.setAverageFinalCompromisedNodes(round(finalCompromised.mean()));
        metrics.setAverageCompromiseRatio(round(compromiseRatio.mean()));
        metrics.setAverageResilienceScore(round(resilience.mean()));
        metrics.setAverageAttackEfficiency(round(attackEfficiency.mean()));
        metrics.setAverageDefenseEfficiency(round(defenseEfficiency.mean()));
        metrics.setAverageDeceptionEffectiveness(round(deceptionEffectiveness.mean()));
        metrics.setAverageMtdEffectiveness(round(mtdEffectiveness.mean()));
        metrics.setAverageMeanTimeToCompromise(round(meanTimeToCompromise.mean()));
        metrics.setAverageAttackPathDepth(round(pathDepth.mean()));

        metrics.setStdDevFinalCompromisedNodes(round(finalCompromised.stdDev()));
        metrics.setStdDevCompromiseRatio(round(compromiseRatio.stdDev()));
        metrics.setStdDevResilienceScore(round(resilience.stdDev()));
        metrics.setStdDevAttackEfficiency(round(attackEfficiency.stdDev()));
        metrics.setStdDevDefenseEfficiency(round(defenseEfficiency.stdDev()));
        metrics.setStdDevMeanTimeToCompromise(round(meanTimeToCompromise.stdDev()));

        return metrics;
    }

    private long resolveSeed(EvaluationScenarioRequest request, int iteration) {
        EvaluationSeedStrategy seedStrategy = request.getSeedStrategy() == null
                ? EvaluationSeedStrategy.VARIED
                : request.getSeedStrategy();

        long defaultBase = SeedUtil.stableHash64(
                "evaluation-default-base",
                request.getScenarioName(),
                String.valueOf(request.getNumNodes()),
                String.valueOf(request.getNumEdges()),
                String.valueOf(request.getAttackBudget()),
                String.valueOf(request.getDefenseBudget()),
                String.valueOf(request.getRecoveryBudget()),
                String.valueOf(request.getRounds()),
                String.valueOf(request.getAlgorithmType()),
                String.valueOf(request.getEnableMTD()),
                String.valueOf(request.getEnableDeception()),
                String.valueOf(request.getRepetitions())
        );

        long baseSeed = request.getBaseSeed() == null ? defaultBase : request.getBaseSeed();

        if (seedStrategy == EvaluationSeedStrategy.FIXED) {
            return baseSeed;
        }

        if (iteration == 0) {
            return baseSeed;
        }

        return SeedUtil.mix64(baseSeed, "evaluation-repeat", iteration);
    }

    private EvaluationRun buildEvaluationRun(
            EvaluationScenario scenario,
            EvaluationScenarioRequest request,
            EvaluationComparisonType comparisonType,
            EvaluationAggregateMetricsResponse metrics,
            List<Long> usedSeeds,
            List<EvaluationComparisonItem> comparisonItems
    ) {
        EvaluationRun run = new EvaluationRun();
        run.setScenario(scenario);
        run.setComparisonType(comparisonType);
        run.setScenarioName(request.getScenarioName());
        run.setNumNodes(request.getNumNodes());
        run.setNumEdges(request.getNumEdges());
        run.setAttackBudget(request.getAttackBudget());
        run.setDefenseBudget(request.getDefenseBudget());
        run.setRecoveryBudget(request.getRecoveryBudget());
        run.setRounds(request.getRounds());
        run.setAlgorithmType(request.getAlgorithmType());
        run.setEnableMtd(Boolean.TRUE.equals(request.getEnableMTD()));
        run.setEnableDeception(Boolean.TRUE.equals(request.getEnableDeception()));
        run.setRepetitions(request.getRepetitions());
        run.setSeedStrategy(request.getSeedStrategy());
        run.setBaseSeed(request.getBaseSeed());
        run.setUsedSeedsJson(writeJson(usedSeeds));

        run.setAverageFinalCompromisedNodes(metrics.getAverageFinalCompromisedNodes());
        run.setAverageCompromiseRatio(metrics.getAverageCompromiseRatio());
        run.setAverageResilienceScore(metrics.getAverageResilienceScore());
        run.setAverageAttackEfficiency(metrics.getAverageAttackEfficiency());
        run.setAverageDefenseEfficiency(metrics.getAverageDefenseEfficiency());
        run.setAverageDeceptionEffectiveness(metrics.getAverageDeceptionEffectiveness());
        run.setAverageMtdEffectiveness(metrics.getAverageMtdEffectiveness());
        run.setAverageMeanTimeToCompromise(metrics.getAverageMeanTimeToCompromise());
        run.setAverageAttackPathDepth(metrics.getAverageAttackPathDepth());
        run.setStdDevFinalCompromisedNodes(metrics.getStdDevFinalCompromisedNodes());
        run.setStdDevCompromiseRatio(metrics.getStdDevCompromiseRatio());
        run.setStdDevResilienceScore(metrics.getStdDevResilienceScore());
        run.setStdDevAttackEfficiency(metrics.getStdDevAttackEfficiency());
        run.setStdDevDefenseEfficiency(metrics.getStdDevDefenseEfficiency());
        run.setStdDevMeanTimeToCompromise(metrics.getStdDevMeanTimeToCompromise());

        run.setComparisonItemsJson(comparisonItems == null ? null : writeJson(comparisonItems));
        run.setCreatedAt(LocalDateTime.now());

        return run;
    }

    private EvaluationRunResponse toResponse(EvaluationRun run) {
        EvaluationRunResponse response = new EvaluationRunResponse();
        response.setEvaluationRunId(run.getId());
        response.setScenarioId(run.getScenario() == null ? null : run.getScenario().getId());
        response.setComparisonType(run.getComparisonType());
        response.setScenarioName(run.getScenarioName());
        response.setNumNodes(run.getNumNodes());
        response.setNumEdges(run.getNumEdges());
        response.setAttackBudget(run.getAttackBudget());
        response.setDefenseBudget(run.getDefenseBudget());
        response.setRecoveryBudget(run.getRecoveryBudget());
        response.setRounds(run.getRounds());
        response.setAlgorithmType(run.getAlgorithmType());
        response.setEnableMTD(run.isEnableMtd());
        response.setEnableDeception(run.isEnableDeception());
        response.setRepetitions(run.getRepetitions());
        response.setSeedStrategy(run.getSeedStrategy());
        response.setBaseSeed(run.getBaseSeed());
        response.setUsedSeeds(readLongList(run.getUsedSeedsJson()));
        response.setCreatedAt(run.getCreatedAt());

        EvaluationAggregateMetricsResponse metrics = new EvaluationAggregateMetricsResponse();
        metrics.setRunsExecuted(run.getRepetitions());
        metrics.setAverageFinalCompromisedNodes(run.getAverageFinalCompromisedNodes());
        metrics.setAverageCompromiseRatio(run.getAverageCompromiseRatio());
        metrics.setAverageResilienceScore(run.getAverageResilienceScore());
        metrics.setAverageAttackEfficiency(run.getAverageAttackEfficiency());
        metrics.setAverageDefenseEfficiency(run.getAverageDefenseEfficiency());
        metrics.setAverageDeceptionEffectiveness(run.getAverageDeceptionEffectiveness());
        metrics.setAverageMtdEffectiveness(run.getAverageMtdEffectiveness());
        metrics.setAverageMeanTimeToCompromise(run.getAverageMeanTimeToCompromise());
        metrics.setAverageAttackPathDepth(run.getAverageAttackPathDepth());
        metrics.setStdDevFinalCompromisedNodes(run.getStdDevFinalCompromisedNodes());
        metrics.setStdDevCompromiseRatio(run.getStdDevCompromiseRatio());
        metrics.setStdDevResilienceScore(run.getStdDevResilienceScore());
        metrics.setStdDevAttackEfficiency(run.getStdDevAttackEfficiency());
        metrics.setStdDevDefenseEfficiency(run.getStdDevDefenseEfficiency());
        metrics.setStdDevMeanTimeToCompromise(run.getStdDevMeanTimeToCompromise());
        response.setAggregateMetrics(metrics);

        return response;
    }

    private EvaluationScenarioRequest copyScenario(EvaluationScenarioRequest source) {
        EvaluationScenarioRequest copy = new EvaluationScenarioRequest();
        copy.setScenarioName(source.getScenarioName());
        copy.setNumNodes(source.getNumNodes());
        copy.setNumEdges(source.getNumEdges());
        copy.setAttackBudget(source.getAttackBudget());
        copy.setDefenseBudget(source.getDefenseBudget());
        copy.setRecoveryBudget(source.getRecoveryBudget());
        copy.setRounds(source.getRounds());
        copy.setAlgorithmType(source.getAlgorithmType());
        copy.setEnableMTD(source.getEnableMTD());
        copy.setEnableDeception(source.getEnableDeception());
        copy.setRepetitions(source.getRepetitions());
        copy.setSeedStrategy(source.getSeedStrategy());
        copy.setBaseSeed(source.getBaseSeed());
        return copy;
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String raw = String.valueOf(value);
        return '"' + raw.replace("\"", "\"\"") + '"';
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize evaluation payload", ex);
        }
    }

    private List<Long> readLongList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse used seeds", ex);
        }
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static class ScenarioExecution {
        private final EvaluationAggregateMetricsResponse aggregateMetrics;
        private final List<Long> usedSeeds;

        private ScenarioExecution(EvaluationAggregateMetricsResponse aggregateMetrics, List<Long> usedSeeds) {
            this.aggregateMetrics = aggregateMetrics;
            this.usedSeeds = usedSeeds;
        }
    }

    private static class StatAccumulator {
        private final List<Double> values = new ArrayList<>();

        private void add(double value) {
            values.add(value);
        }

        private double mean() {
            if (values.isEmpty()) {
                return 0.0;
            }
            return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        private double stdDev() {
            int n = values.size();
            if (n <= 1) {
                return 0.0;
            }
            double mean = mean();
            double sse = values.stream().mapToDouble(value -> {
                double diff = value - mean;
                return diff * diff;
            }).sum();
            return Math.sqrt(sse / (n - 1));
        }
    }
}
