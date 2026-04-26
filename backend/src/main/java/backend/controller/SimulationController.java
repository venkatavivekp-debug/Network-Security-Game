package backend.controller;

import backend.dto.AdvancedSimulationRunRequest;
import backend.dto.AdvancedSimulationRunResponse;
import backend.dto.EvaluationComparisonResponse;
import backend.dto.EvaluationDefenseCompareMode;
import backend.dto.EvaluationScenarioRequest;
import backend.dto.EvaluationRunResponse;
import backend.dto.SimulationComparisonResponse;
import backend.dto.SimulationHistoryResponse;
import backend.dto.SimulationRunRequest;
import backend.dto.SimulationRunResponse;
import backend.dto.ApiSuccessResponse;
import backend.model.AlgorithmType;
import backend.model.EvaluationComparisonType;
import backend.model.EvaluationSeedStrategy;
import backend.model.Message;
import backend.model.SimulationRun;
import backend.service.MessageService;
import backend.service.AdvancedSimulationService;
import backend.service.EvaluationFrameworkService;
import backend.service.SimulationComparisonService;
import backend.service.SimulationHistoryService;
import backend.simulation.game.GameSimulationService;
import backend.simulation.game.SimulationResult;
import backend.util.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/simulation")
@Validated
public class SimulationController {

    private final GameSimulationService gameSimulationService;
    private final MessageService messageService;
    private final SimulationHistoryService simulationHistoryService;
    private final SimulationComparisonService simulationComparisonService;
    private final AdvancedSimulationService advancedSimulationService;
    private final EvaluationFrameworkService evaluationFrameworkService;

    public SimulationController(
            GameSimulationService gameSimulationService,
            MessageService messageService,
            SimulationHistoryService simulationHistoryService,
            SimulationComparisonService simulationComparisonService,
            AdvancedSimulationService advancedSimulationService,
            EvaluationFrameworkService evaluationFrameworkService
    ) {
        this.gameSimulationService = gameSimulationService;
        this.messageService = messageService;
        this.simulationHistoryService = simulationHistoryService;
        this.simulationComparisonService = simulationComparisonService;
        this.advancedSimulationService = advancedSimulationService;
        this.evaluationFrameworkService = evaluationFrameworkService;
    }

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<SimulationRunResponse>> run(
            @Valid @RequestBody SimulationRunRequest request,
            org.springframework.security.core.Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        AlgorithmType algorithmType = resolveAlgorithmType(request, authentication);

        SimulationResult result = gameSimulationService.runSimulation(
                request.getNumNodes(),
                request.getNumEdges(),
                request.getAttackBudget(),
                request.getDefenseBudget(),
                request.getRecoveryBudget(),
                algorithmType
        );

        SimulationRun savedRun = simulationHistoryService.saveRun(
                request.getNumNodes(),
                request.getNumEdges(),
                request.getAttackBudget(),
                request.getDefenseBudget(),
                request.getRecoveryBudget(),
                algorithmType,
                request.getMessageId(),
                result
        );

        return ResponseEntity.ok(ApiResponseUtil.success(
                "Simulation run completed",
                httpRequest.getRequestURI(),
                simulationHistoryService.toRunResponse(savedRun)
        ));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<List<SimulationHistoryResponse>>> history(
            @RequestParam(value = "algorithmType", required = false) AlgorithmType algorithmType,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Simulation history fetched",
                httpRequest.getRequestURI(),
                simulationHistoryService.getHistory(algorithmType)
        ));
    }

    @GetMapping("/history/{id}")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<SimulationHistoryResponse>> historyById(
            @PathVariable("id") @Positive(message = "id must be positive") Long id,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Simulation history item fetched",
                httpRequest.getRequestURI(),
                simulationHistoryService.getHistoryById(id)
        ));
    }

    @GetMapping("/compare")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<SimulationComparisonResponse>> compare(
            @RequestParam("numNodes") @Min(value = 2, message = "numNodes must be at least 2") @Max(value = 300, message = "numNodes must be at most 300") int numNodes,
            @RequestParam("numEdges") @Min(value = 0, message = "numEdges cannot be negative") @Max(value = 50000, message = "numEdges is too large") int numEdges,
            @RequestParam("attackBudget") @Min(value = 0, message = "attackBudget cannot be negative") @Max(value = 10000, message = "attackBudget is too large") int attackBudget,
            @RequestParam("defenseBudget") @Min(value = 0, message = "defenseBudget cannot be negative") @Max(value = 10000, message = "defenseBudget is too large") int defenseBudget,
            @RequestParam("recoveryBudget") @Min(value = 0, message = "recoveryBudget cannot be negative") @Max(value = 10000, message = "recoveryBudget is too large") int recoveryBudget,
            HttpServletRequest httpRequest
    ) {
        SimulationComparisonResponse response = simulationComparisonService.compareAndOptionallyPersist(
                numNodes,
                numEdges,
                attackBudget,
                defenseBudget,
                recoveryBudget,
                true
        );

        return ResponseEntity.ok(ApiResponseUtil.success(
                "Simulation comparison completed",
                httpRequest.getRequestURI(),
                response
        ));
    }

    @PostMapping("/advanced-run")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<AdvancedSimulationRunResponse>> advancedRun(
            @Valid @RequestBody AdvancedSimulationRunRequest request,
            HttpServletRequest httpRequest
    ) {
        AdvancedSimulationRunResponse response = advancedSimulationService.runAndPersist(request);
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Advanced simulation run completed",
                httpRequest.getRequestURI(),
                response
        ));
    }

    @GetMapping("/advanced-history")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<List<AdvancedSimulationRunResponse>>> advancedHistory(
            @RequestParam(value = "algorithmType", required = false) AlgorithmType algorithmType,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Advanced simulation history fetched",
                httpRequest.getRequestURI(),
                advancedSimulationService.getHistory(algorithmType)
        ));
    }

    @GetMapping("/advanced-history/{id}")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<AdvancedSimulationRunResponse>> advancedHistoryById(
            @PathVariable("id") @Positive(message = "id must be positive") Long id,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Advanced simulation history item fetched",
                httpRequest.getRequestURI(),
                advancedSimulationService.getHistoryById(id)
        ));
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<EvaluationRunResponse>> evaluate(
            @Valid @RequestBody EvaluationScenarioRequest request,
            HttpServletRequest httpRequest
    ) {
        EvaluationRunResponse response = evaluationFrameworkService.evaluateAndPersist(request);
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Evaluation run completed",
                httpRequest.getRequestURI(),
                response
        ));
    }

    @GetMapping("/evaluations")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<List<EvaluationRunResponse>>> evaluations(
            @RequestParam(value = "algorithmType", required = false) AlgorithmType algorithmType,
            @RequestParam(value = "comparisonType", required = false) EvaluationComparisonType comparisonType,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Evaluation history fetched",
                httpRequest.getRequestURI(),
                evaluationFrameworkService.getEvaluations(algorithmType, comparisonType)
        ));
    }

    @GetMapping("/evaluations/{id}")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<EvaluationRunResponse>> evaluationById(
            @PathVariable("id") @Positive(message = "id must be positive") Long id,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Evaluation history item fetched",
                httpRequest.getRequestURI(),
                evaluationFrameworkService.getEvaluationById(id)
        ));
    }

    @GetMapping("/evaluate/compare-security")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<EvaluationComparisonResponse>> compareSecurity(
            @RequestParam(value = "scenarioName", defaultValue = "Security Algorithm Benchmark") String scenarioName,
            @RequestParam("numNodes") @Min(value = 2, message = "numNodes must be at least 2") @Max(value = 500, message = "numNodes must be at most 500") int numNodes,
            @RequestParam("numEdges") @Min(value = 0, message = "numEdges cannot be negative") @Max(value = 100000, message = "numEdges is too large") int numEdges,
            @RequestParam("attackBudget") @Min(value = 0, message = "attackBudget cannot be negative") @Max(value = 10000, message = "attackBudget is too large") int attackBudget,
            @RequestParam("defenseBudget") @Min(value = 0, message = "defenseBudget cannot be negative") @Max(value = 10000, message = "defenseBudget is too large") int defenseBudget,
            @RequestParam("recoveryBudget") @Min(value = 0, message = "recoveryBudget cannot be negative") @Max(value = 10000, message = "recoveryBudget is too large") int recoveryBudget,
            @RequestParam("rounds") @Min(value = 1, message = "rounds must be at least 1") @Max(value = 100, message = "rounds must be at most 100") int rounds,
            @RequestParam(value = "enableMTD", defaultValue = "true") boolean enableMTD,
            @RequestParam(value = "enableDeception", defaultValue = "true") boolean enableDeception,
            @RequestParam(value = "repetitions", defaultValue = "10") @Min(value = 1, message = "repetitions must be at least 1") @Max(value = 200, message = "repetitions must be at most 200") int repetitions,
            @RequestParam(value = "seedStrategy", defaultValue = "VARIED") EvaluationSeedStrategy seedStrategy,
            @RequestParam(value = "baseSeed", required = false) @Min(value = 0, message = "baseSeed must be non-negative") Long baseSeed,
            HttpServletRequest httpRequest
    ) {
        EvaluationScenarioRequest scenario = buildEvaluationScenario(
                scenarioName,
                numNodes,
                numEdges,
                attackBudget,
                defenseBudget,
                recoveryBudget,
                rounds,
                AlgorithmType.NORMAL,
                enableMTD,
                enableDeception,
                repetitions,
                seedStrategy,
                baseSeed
        );

        EvaluationComparisonResponse response = evaluationFrameworkService.compareSecurity(scenario);
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Security comparison completed",
                httpRequest.getRequestURI(),
                response
        ));
    }

    @GetMapping("/evaluate/compare-defense")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<EvaluationComparisonResponse>> compareDefense(
            @RequestParam(value = "scenarioName", defaultValue = "Defense Benchmark") String scenarioName,
            @RequestParam("mode") EvaluationDefenseCompareMode mode,
            @RequestParam("numNodes") @Min(value = 2, message = "numNodes must be at least 2") @Max(value = 500, message = "numNodes must be at most 500") int numNodes,
            @RequestParam("numEdges") @Min(value = 0, message = "numEdges cannot be negative") @Max(value = 100000, message = "numEdges is too large") int numEdges,
            @RequestParam("attackBudget") @Min(value = 0, message = "attackBudget cannot be negative") @Max(value = 10000, message = "attackBudget is too large") int attackBudget,
            @RequestParam("defenseBudget") @Min(value = 0, message = "defenseBudget cannot be negative") @Max(value = 10000, message = "defenseBudget is too large") int defenseBudget,
            @RequestParam("recoveryBudget") @Min(value = 0, message = "recoveryBudget cannot be negative") @Max(value = 10000, message = "recoveryBudget is too large") int recoveryBudget,
            @RequestParam("rounds") @Min(value = 1, message = "rounds must be at least 1") @Max(value = 100, message = "rounds must be at most 100") int rounds,
            @RequestParam(value = "algorithmType", defaultValue = "CPHS") AlgorithmType algorithmType,
            @RequestParam(value = "enableMTD", defaultValue = "true") boolean enableMTD,
            @RequestParam(value = "enableDeception", defaultValue = "true") boolean enableDeception,
            @RequestParam(value = "repetitions", defaultValue = "10") @Min(value = 1, message = "repetitions must be at least 1") @Max(value = 200, message = "repetitions must be at most 200") int repetitions,
            @RequestParam(value = "seedStrategy", defaultValue = "VARIED") EvaluationSeedStrategy seedStrategy,
            @RequestParam(value = "baseSeed", required = false) @Min(value = 0, message = "baseSeed must be non-negative") Long baseSeed,
            @RequestParam(value = "sweepStart", required = false) Integer sweepStart,
            @RequestParam(value = "sweepEnd", required = false) Integer sweepEnd,
            @RequestParam(value = "sweepStep", required = false) Integer sweepStep,
            HttpServletRequest httpRequest
    ) {
        EvaluationScenarioRequest scenario = buildEvaluationScenario(
                scenarioName,
                numNodes,
                numEdges,
                attackBudget,
                defenseBudget,
                recoveryBudget,
                rounds,
                algorithmType,
                enableMTD,
                enableDeception,
                repetitions,
                seedStrategy,
                baseSeed
        );

        EvaluationComparisonResponse response = evaluationFrameworkService.compareDefense(
                scenario,
                mode,
                sweepStart,
                sweepEnd,
                sweepStep
        );

        return ResponseEntity.ok(ApiResponseUtil.success(
                "Defense comparison completed",
                httpRequest.getRequestURI(),
                response
        ));
    }

    @GetMapping("/evaluate/export")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<byte[]> exportEvaluation(
            @RequestParam(value = "algorithmType", required = false) AlgorithmType algorithmType,
            @RequestParam(value = "comparisonType", required = false) EvaluationComparisonType comparisonType
    ) {
        byte[] csv = evaluationFrameworkService.exportEvaluationsCsv(algorithmType, comparisonType);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String filename = "evaluation-benchmarks-" + timestamp + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "algorithmType", required = false) AlgorithmType algorithmType
    ) {
        List<SimulationHistoryResponse> history = simulationHistoryService.getHistory(algorithmType);
        String csv = buildCsv(history);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = algorithmType == null
                ? "simulation-history-" + timestamp + ".csv"
                : "simulation-history-" + algorithmType + "-" + timestamp + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private String buildCsv(List<SimulationHistoryResponse> history) {
        StringBuilder builder = new StringBuilder();
        builder.append("id,createdAt,algorithmType,messageId,numNodes,numEdges,attackBudget,defenseBudget,recoveryBudget,")
                .append("initialConnectivity,afterAttackConnectivity,afterRecoveryConnectivity,nodesLost,edgesLost,")
                .append("recoveryRate,defenderUtility,attackerUtility,effectiveAttackSuccessProbability")
                .append("\n");

        for (SimulationHistoryResponse run : history) {
            builder.append(csvValue(run.getId())).append(',')
                    .append(csvValue(run.getCreatedAt())).append(',')
                    .append(csvValue(run.getAlgorithmType())).append(',')
                    .append(csvValue(run.getMessageId())).append(',')
                    .append(csvValue(run.getNumNodes())).append(',')
                    .append(csvValue(run.getNumEdges())).append(',')
                    .append(csvValue(run.getAttackBudget())).append(',')
                    .append(csvValue(run.getDefenseBudget())).append(',')
                    .append(csvValue(run.getRecoveryBudget())).append(',')
                    .append(csvValue(run.getInitialConnectivity())).append(',')
                    .append(csvValue(run.getAfterAttackConnectivity())).append(',')
                    .append(csvValue(run.getAfterRecoveryConnectivity())).append(',')
                    .append(csvValue(run.getNodesLost())).append(',')
                    .append(csvValue(run.getEdgesLost())).append(',')
                    .append(csvValue(run.getRecoveryRate())).append(',')
                    .append(csvValue(run.getDefenderUtility())).append(',')
                    .append(csvValue(run.getAttackerUtility())).append(',')
                    .append(csvValue(run.getEffectiveAttackSuccessProbability()))
                    .append('\n');
        }

        return builder.toString();
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String raw = String.valueOf(value);
        String escaped = raw.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }

    private AlgorithmType resolveAlgorithmType(SimulationRunRequest request, org.springframework.security.core.Authentication authentication) {
        if (request.getMessageId() != null) {
            // Participant-scoped lookup so a sender or receiver cannot run a
            // simulation against an arbitrary message id and learn its mode.
            Message message = messageService.getByIdForUser(request.getMessageId(), authentication.getName());
            return message.getAlgorithmType();
        }

        if (request.getAlgorithmType() != null) {
            return request.getAlgorithmType();
        }

        return AlgorithmType.NORMAL;
    }

    private EvaluationScenarioRequest buildEvaluationScenario(
            String scenarioName,
            int numNodes,
            int numEdges,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget,
            int rounds,
            AlgorithmType algorithmType,
            boolean enableMTD,
            boolean enableDeception,
            int repetitions,
            EvaluationSeedStrategy seedStrategy,
            Long baseSeed
    ) {
        EvaluationScenarioRequest scenario = new EvaluationScenarioRequest();
        scenario.setScenarioName(scenarioName);
        scenario.setNumNodes(numNodes);
        scenario.setNumEdges(numEdges);
        scenario.setAttackBudget(attackBudget);
        scenario.setDefenseBudget(defenseBudget);
        scenario.setRecoveryBudget(recoveryBudget);
        scenario.setRounds(rounds);
        scenario.setAlgorithmType(algorithmType);
        scenario.setEnableMTD(enableMTD);
        scenario.setEnableDeception(enableDeception);
        scenario.setRepetitions(repetitions);
        scenario.setSeedStrategy(seedStrategy);
        scenario.setBaseSeed(baseSeed);
        return scenario;
    }
}
