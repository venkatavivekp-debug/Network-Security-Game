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
import backend.exception.BadRequestException;
import backend.model.AlgorithmType;
import backend.model.EvaluationComparisonType;
import backend.model.EvaluationSeedStrategy;
import backend.model.SimulationRun;
import backend.service.AdvancedSimulationService;
import backend.service.EvaluationFrameworkService;
import backend.service.SimulationComparisonService;
import backend.service.SimulationHistoryService;
import backend.simulation.game.GameSimulationService;
import backend.simulation.game.SimulationResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/simulation")
@Validated
public class SimulationPageController {

    private static final int DEFAULT_NUM_NODES = 10;
    private static final int DEFAULT_NUM_EDGES = 15;
    private static final int DEFAULT_ATTACK_BUDGET = 3;
    private static final int DEFAULT_DEFENSE_BUDGET = 3;
    private static final int DEFAULT_RECOVERY_BUDGET = 2;
    private static final int DEFAULT_ADVANCED_ROUNDS = 10;
    private static final int DEFAULT_EVAL_REPETITIONS = 10;

    private final GameSimulationService gameSimulationService;
    private final SimulationHistoryService simulationHistoryService;
    private final SimulationComparisonService simulationComparisonService;
    private final AdvancedSimulationService advancedSimulationService;
    private final EvaluationFrameworkService evaluationFrameworkService;

    public SimulationPageController(
            GameSimulationService gameSimulationService,
            SimulationHistoryService simulationHistoryService,
            SimulationComparisonService simulationComparisonService,
            AdvancedSimulationService advancedSimulationService,
            EvaluationFrameworkService evaluationFrameworkService
    ) {
        this.gameSimulationService = gameSimulationService;
        this.simulationHistoryService = simulationHistoryService;
        this.simulationComparisonService = simulationComparisonService;
        this.advancedSimulationService = advancedSimulationService;
        this.evaluationFrameworkService = evaluationFrameworkService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String dashboard(Model model) {
        SimulationRunRequest form = ensureDashboardForm(model);
        model.addAttribute("algorithms", AlgorithmType.values());

        if (!model.containsAttribute("comparisonResult")) {
            int numNodes = safeInt(form.getNumNodes(), DEFAULT_NUM_NODES);
            int numEdges = safeInt(form.getNumEdges(), DEFAULT_NUM_EDGES);
            int attackBudget = safeInt(form.getAttackBudget(), DEFAULT_ATTACK_BUDGET);
            int defenseBudget = safeInt(form.getDefenseBudget(), DEFAULT_DEFENSE_BUDGET);
            int recoveryBudget = safeInt(form.getRecoveryBudget(), DEFAULT_RECOVERY_BUDGET);

            SimulationComparisonResponse defaultComparison = simulationComparisonService.compareAndOptionallyPersist(
                    numNodes,
                    numEdges,
                    attackBudget,
                    defenseBudget,
                    recoveryBudget,
                    false
            );
            model.addAttribute("comparisonResult", defaultComparison);
        }

        if (!model.containsAttribute("historyPreview")) {
            List<SimulationHistoryResponse> historyPreview = simulationHistoryService.getHistory(null)
                    .stream()
                    .limit(5)
                    .toList();
            model.addAttribute("historyPreview", historyPreview);
        }

        return "simulation";
    }

    @PostMapping("/dashboard/run")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String runFromDashboard(
            @RequestParam("numNodes") @Min(value = 2, message = "numNodes must be at least 2") @Max(value = 300, message = "numNodes must be at most 300") int numNodes,
            @RequestParam("numEdges") @Min(value = 0, message = "numEdges cannot be negative") @Max(value = 50000, message = "numEdges is too large") int numEdges,
            @RequestParam("attackBudget") @Min(value = 0, message = "attackBudget cannot be negative") @Max(value = 10000, message = "attackBudget is too large") int attackBudget,
            @RequestParam("defenseBudget") @Min(value = 0, message = "defenseBudget cannot be negative") @Max(value = 10000, message = "defenseBudget is too large") int defenseBudget,
            @RequestParam("recoveryBudget") @Min(value = 0, message = "recoveryBudget cannot be negative") @Max(value = 10000, message = "recoveryBudget is too large") int recoveryBudget,
            @RequestParam("algorithmType") AlgorithmType algorithmType,
            RedirectAttributes redirectAttributes
    ) {
        SimulationResult result = gameSimulationService.runSimulation(
                numNodes,
                numEdges,
                attackBudget,
                defenseBudget,
                recoveryBudget,
                algorithmType
        );

        SimulationRun savedRun = simulationHistoryService.saveRun(
                numNodes,
                numEdges,
                attackBudget,
                defenseBudget,
                recoveryBudget,
                algorithmType,
                null,
                result
        );

        SimulationRunResponse runResponse = simulationHistoryService.toRunResponse(savedRun);

        SimulationComparisonResponse comparisonResult = simulationComparisonService.compareAndOptionallyPersist(
                numNodes,
                numEdges,
                attackBudget,
                defenseBudget,
                recoveryBudget,
                false
        );

        SimulationRunRequest form = new SimulationRunRequest();
        form.setNumNodes(numNodes);
        form.setNumEdges(numEdges);
        form.setAttackBudget(attackBudget);
        form.setDefenseBudget(defenseBudget);
        form.setRecoveryBudget(recoveryBudget);
        form.setAlgorithmType(algorithmType);

        redirectAttributes.addFlashAttribute("form", form);
        redirectAttributes.addFlashAttribute("currentResult", runResponse);
        redirectAttributes.addFlashAttribute("comparisonResult", comparisonResult);
        redirectAttributes.addFlashAttribute("notice", "Simulation run completed and saved to history");

        return "redirect:/simulation/dashboard";
    }

    @GetMapping("/history-page")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String historyPage(
            @RequestParam(value = "algorithmType", required = false) AlgorithmType algorithmType,
            Model model
    ) {
        model.addAttribute("selectedAlgorithm", algorithmType);
        model.addAttribute("algorithms", AlgorithmType.values());
        model.addAttribute("runs", simulationHistoryService.getHistory(algorithmType));
        return "simulation-history";
    }

    @GetMapping("/history-page/{id}")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String historyDetails(@PathVariable("id") @Positive(message = "id must be positive") Long runId, Model model) {
        model.addAttribute("run", simulationHistoryService.getHistoryById(runId));
        return "simulation-history-detail";
    }

    @GetMapping("/compare-page")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String comparePage(Model model) {
        if (!model.containsAttribute("compareForm")) {
            CompareForm form = new CompareForm();
            form.setNumNodes(DEFAULT_NUM_NODES);
            form.setNumEdges(DEFAULT_NUM_EDGES);
            form.setAttackBudget(DEFAULT_ATTACK_BUDGET);
            form.setDefenseBudget(DEFAULT_DEFENSE_BUDGET);
            form.setRecoveryBudget(DEFAULT_RECOVERY_BUDGET);
            model.addAttribute("compareForm", form);
        }

        return "simulation-compare";
    }

    @PostMapping("/compare-page/run")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String runComparisonFromPage(
            @RequestParam("numNodes") @Min(value = 2, message = "numNodes must be at least 2") @Max(value = 300, message = "numNodes must be at most 300") int numNodes,
            @RequestParam("numEdges") @Min(value = 0, message = "numEdges cannot be negative") @Max(value = 50000, message = "numEdges is too large") int numEdges,
            @RequestParam("attackBudget") @Min(value = 0, message = "attackBudget cannot be negative") @Max(value = 10000, message = "attackBudget is too large") int attackBudget,
            @RequestParam("defenseBudget") @Min(value = 0, message = "defenseBudget cannot be negative") @Max(value = 10000, message = "defenseBudget is too large") int defenseBudget,
            @RequestParam("recoveryBudget") @Min(value = 0, message = "recoveryBudget cannot be negative") @Max(value = 10000, message = "recoveryBudget is too large") int recoveryBudget,
            RedirectAttributes redirectAttributes
    ) {
        SimulationComparisonResponse comparisonResult = simulationComparisonService.compareAndOptionallyPersist(
                numNodes,
                numEdges,
                attackBudget,
                defenseBudget,
                recoveryBudget,
                true
        );

        CompareForm form = new CompareForm();
        form.setNumNodes(numNodes);
        form.setNumEdges(numEdges);
        form.setAttackBudget(attackBudget);
        form.setDefenseBudget(defenseBudget);
        form.setRecoveryBudget(recoveryBudget);

        redirectAttributes.addFlashAttribute("compareForm", form);
        redirectAttributes.addFlashAttribute("comparisonResult", comparisonResult);
        redirectAttributes.addFlashAttribute("notice", "Scenario comparison run completed and saved to history");

        return "redirect:/simulation/compare-page";
    }

    @GetMapping("/advanced-dashboard")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String advancedDashboard(Model model) {
        if (!model.containsAttribute("advancedForm")) {
            AdvancedSimulationRunRequest form = new AdvancedSimulationRunRequest();
            form.setNumNodes(20);
            form.setNumEdges(35);
            form.setAttackBudget(6);
            form.setDefenseBudget(6);
            form.setRecoveryBudget(3);
            form.setRounds(DEFAULT_ADVANCED_ROUNDS);
            form.setEnableMTD(true);
            form.setEnableDeception(true);
            form.setAlgorithmType(AlgorithmType.CPHS);
            model.addAttribute("advancedForm", form);
        }

        if (!model.containsAttribute("advancedHistoryPreview")) {
            List<AdvancedSimulationRunResponse> history = advancedSimulationService.getHistory(null)
                    .stream()
                    .limit(5)
                    .toList();
            model.addAttribute("advancedHistoryPreview", history);
        }

        model.addAttribute("algorithms", AlgorithmType.values());
        return "advanced-simulation";
    }

    @PostMapping("/advanced-dashboard/run")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String runAdvancedFromDashboard(
            @RequestParam("numNodes") @Min(value = 2, message = "numNodes must be at least 2") @Max(value = 500, message = "numNodes must be at most 500") int numNodes,
            @RequestParam("numEdges") @Min(value = 0, message = "numEdges cannot be negative") @Max(value = 100000, message = "numEdges is too large") int numEdges,
            @RequestParam("attackBudget") @Min(value = 0, message = "attackBudget cannot be negative") @Max(value = 10000, message = "attackBudget is too large") int attackBudget,
            @RequestParam("defenseBudget") @Min(value = 0, message = "defenseBudget cannot be negative") @Max(value = 10000, message = "defenseBudget is too large") int defenseBudget,
            @RequestParam("recoveryBudget") @Min(value = 0, message = "recoveryBudget cannot be negative") @Max(value = 10000, message = "recoveryBudget is too large") int recoveryBudget,
            @RequestParam("rounds") @Min(value = 1, message = "rounds must be at least 1") @Max(value = 100, message = "rounds must be at most 100") int rounds,
            @RequestParam(value = "enableMTD", defaultValue = "false") boolean enableMTD,
            @RequestParam(value = "enableDeception", defaultValue = "false") boolean enableDeception,
            @RequestParam("algorithmType") AlgorithmType algorithmType,
            @RequestParam(value = "seed", required = false) Long seed,
            RedirectAttributes redirectAttributes
    ) {
        int maxDirectedEdges = numNodes * (numNodes - 1);
        if (numEdges > maxDirectedEdges) {
            throw new BadRequestException("numEdges exceeds maximum possible directed edges (" + maxDirectedEdges + ") for numNodes=" + numNodes);
        }

        AdvancedSimulationRunRequest request = new AdvancedSimulationRunRequest();
        request.setNumNodes(numNodes);
        request.setNumEdges(numEdges);
        request.setAttackBudget(attackBudget);
        request.setDefenseBudget(defenseBudget);
        request.setRecoveryBudget(recoveryBudget);
        request.setRounds(rounds);
        request.setEnableMTD(enableMTD);
        request.setEnableDeception(enableDeception);
        request.setAlgorithmType(algorithmType);
        request.setSeed(seed);

        AdvancedSimulationRunResponse response = advancedSimulationService.runAndPersist(request);

        redirectAttributes.addFlashAttribute("advancedForm", request);
        redirectAttributes.addFlashAttribute("advancedResult", response);
        redirectAttributes.addFlashAttribute("notice", "Advanced simulation run completed and saved to history");
        return "redirect:/simulation/advanced-dashboard";
    }

    @GetMapping("/evaluation-dashboard")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String evaluationDashboard(Model model) {
        if (!model.containsAttribute("evaluationForm")) {
            EvaluationScenarioRequest form = new EvaluationScenarioRequest();
            form.setScenarioName("Baseline Evaluation");
            form.setNumNodes(20);
            form.setNumEdges(35);
            form.setAttackBudget(6);
            form.setDefenseBudget(6);
            form.setRecoveryBudget(3);
            form.setRounds(10);
            form.setAlgorithmType(AlgorithmType.CPHS);
            form.setEnableMTD(true);
            form.setEnableDeception(true);
            form.setRepetitions(DEFAULT_EVAL_REPETITIONS);
            form.setSeedStrategy(EvaluationSeedStrategy.VARIED);
            model.addAttribute("evaluationForm", form);
        }

        if (!model.containsAttribute("evaluationHistoryPreview")) {
            List<EvaluationRunResponse> history = evaluationFrameworkService.getEvaluations(null, null)
                    .stream()
                    .limit(5)
                    .toList();
            model.addAttribute("evaluationHistoryPreview", history);
        }

        model.addAttribute("algorithms", AlgorithmType.values());
        model.addAttribute("seedStrategies", EvaluationSeedStrategy.values());
        return "evaluation-dashboard";
    }

    @PostMapping("/evaluation-dashboard/run")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String runEvaluationFromDashboard(
            @RequestParam("scenarioName") String scenarioName,
            @RequestParam("numNodes") @Min(value = 2, message = "numNodes must be at least 2") @Max(value = 500, message = "numNodes must be at most 500") int numNodes,
            @RequestParam("numEdges") @Min(value = 0, message = "numEdges cannot be negative") @Max(value = 100000, message = "numEdges is too large") int numEdges,
            @RequestParam("attackBudget") @Min(value = 0, message = "attackBudget cannot be negative") @Max(value = 10000, message = "attackBudget is too large") int attackBudget,
            @RequestParam("defenseBudget") @Min(value = 0, message = "defenseBudget cannot be negative") @Max(value = 10000, message = "defenseBudget is too large") int defenseBudget,
            @RequestParam("recoveryBudget") @Min(value = 0, message = "recoveryBudget cannot be negative") @Max(value = 10000, message = "recoveryBudget is too large") int recoveryBudget,
            @RequestParam("rounds") @Min(value = 1, message = "rounds must be at least 1") @Max(value = 100, message = "rounds must be at most 100") int rounds,
            @RequestParam("algorithmType") AlgorithmType algorithmType,
            @RequestParam(value = "enableMTD", defaultValue = "false") boolean enableMTD,
            @RequestParam(value = "enableDeception", defaultValue = "false") boolean enableDeception,
            @RequestParam("repetitions") @Min(value = 1, message = "repetitions must be at least 1") @Max(value = 200, message = "repetitions must be at most 200") int repetitions,
            @RequestParam("seedStrategy") EvaluationSeedStrategy seedStrategy,
            @RequestParam(value = "baseSeed", required = false) Long baseSeed,
            RedirectAttributes redirectAttributes
    ) {
        int maxDirectedEdges = numNodes * (numNodes - 1);
        if (numEdges > maxDirectedEdges) {
            throw new BadRequestException("numEdges exceeds maximum possible directed edges (" + maxDirectedEdges + ") for numNodes=" + numNodes);
        }
        if (baseSeed != null && baseSeed < 0) {
            throw new BadRequestException("baseSeed must be non-negative");
        }

        EvaluationScenarioRequest request = new EvaluationScenarioRequest();
        request.setScenarioName(scenarioName);
        request.setNumNodes(numNodes);
        request.setNumEdges(numEdges);
        request.setAttackBudget(attackBudget);
        request.setDefenseBudget(defenseBudget);
        request.setRecoveryBudget(recoveryBudget);
        request.setRounds(rounds);
        request.setAlgorithmType(algorithmType);
        request.setEnableMTD(enableMTD);
        request.setEnableDeception(enableDeception);
        request.setRepetitions(repetitions);
        request.setSeedStrategy(seedStrategy);
        request.setBaseSeed(baseSeed);

        EvaluationRunResponse response = evaluationFrameworkService.evaluateAndPersist(request);

        redirectAttributes.addFlashAttribute("evaluationForm", request);
        redirectAttributes.addFlashAttribute("evaluationResult", response);
        redirectAttributes.addFlashAttribute("notice", "Evaluation run completed and saved to history");
        return "redirect:/simulation/evaluation-dashboard";
    }

    @GetMapping("/evaluation-history")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String evaluationHistory(
            @RequestParam(value = "algorithmType", required = false) AlgorithmType algorithmType,
            @RequestParam(value = "comparisonType", required = false) EvaluationComparisonType comparisonType,
            Model model
    ) {
        model.addAttribute("selectedAlgorithm", algorithmType);
        model.addAttribute("selectedComparisonType", comparisonType);
        model.addAttribute("algorithms", AlgorithmType.values());
        model.addAttribute("comparisonTypes", EvaluationComparisonType.values());
        model.addAttribute("runs", evaluationFrameworkService.getEvaluations(algorithmType, comparisonType));
        return "evaluation-history";
    }

    @GetMapping("/evaluation-history/{id}")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String evaluationDetails(@PathVariable("id") @Positive(message = "id must be positive") Long runId, Model model) {
        model.addAttribute("run", evaluationFrameworkService.getEvaluationById(runId));
        return "evaluation-detail";
    }

    @GetMapping("/evaluation-compare")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String evaluationCompare(Model model) {
        if (!model.containsAttribute("securityForm")) {
            EvaluationScenarioRequest securityForm = new EvaluationScenarioRequest();
            securityForm.setScenarioName("Security Comparison");
            securityForm.setNumNodes(20);
            securityForm.setNumEdges(35);
            securityForm.setAttackBudget(6);
            securityForm.setDefenseBudget(6);
            securityForm.setRecoveryBudget(3);
            securityForm.setRounds(10);
            securityForm.setAlgorithmType(AlgorithmType.NORMAL);
            securityForm.setEnableMTD(true);
            securityForm.setEnableDeception(true);
            securityForm.setRepetitions(DEFAULT_EVAL_REPETITIONS);
            securityForm.setSeedStrategy(EvaluationSeedStrategy.VARIED);
            model.addAttribute("securityForm", securityForm);
        }

        if (!model.containsAttribute("defenseMode")) {
            model.addAttribute("defenseMode", EvaluationDefenseCompareMode.MTD);
        }
        if (!model.containsAttribute("sweepStart")) {
            model.addAttribute("sweepStart", 2);
            model.addAttribute("sweepEnd", 10);
            model.addAttribute("sweepStep", 2);
        }

        model.addAttribute("algorithms", AlgorithmType.values());
        model.addAttribute("seedStrategies", EvaluationSeedStrategy.values());
        model.addAttribute("defenseModes", EvaluationDefenseCompareMode.values());
        return "evaluation-compare";
    }

    @PostMapping("/evaluation-compare/security")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String runSecurityComparisonFromPage(
            @RequestParam("scenarioName") String scenarioName,
            @RequestParam("numNodes") @Min(value = 2, message = "numNodes must be at least 2") @Max(value = 500, message = "numNodes must be at most 500") int numNodes,
            @RequestParam("numEdges") @Min(value = 0, message = "numEdges cannot be negative") @Max(value = 100000, message = "numEdges is too large") int numEdges,
            @RequestParam("attackBudget") @Min(value = 0, message = "attackBudget cannot be negative") @Max(value = 10000, message = "attackBudget is too large") int attackBudget,
            @RequestParam("defenseBudget") @Min(value = 0, message = "defenseBudget cannot be negative") @Max(value = 10000, message = "defenseBudget is too large") int defenseBudget,
            @RequestParam("recoveryBudget") @Min(value = 0, message = "recoveryBudget cannot be negative") @Max(value = 10000, message = "recoveryBudget is too large") int recoveryBudget,
            @RequestParam("rounds") @Min(value = 1, message = "rounds must be at least 1") @Max(value = 100, message = "rounds must be at most 100") int rounds,
            @RequestParam(value = "enableMTD", defaultValue = "false") boolean enableMTD,
            @RequestParam(value = "enableDeception", defaultValue = "false") boolean enableDeception,
            @RequestParam("repetitions") @Min(value = 1, message = "repetitions must be at least 1") @Max(value = 200, message = "repetitions must be at most 200") int repetitions,
            @RequestParam("seedStrategy") EvaluationSeedStrategy seedStrategy,
            @RequestParam(value = "baseSeed", required = false) Long baseSeed,
            RedirectAttributes redirectAttributes
    ) {
        EvaluationScenarioRequest scenario = new EvaluationScenarioRequest();
        scenario.setScenarioName(scenarioName);
        scenario.setNumNodes(numNodes);
        scenario.setNumEdges(numEdges);
        scenario.setAttackBudget(attackBudget);
        scenario.setDefenseBudget(defenseBudget);
        scenario.setRecoveryBudget(recoveryBudget);
        scenario.setRounds(rounds);
        scenario.setAlgorithmType(AlgorithmType.NORMAL);
        scenario.setEnableMTD(enableMTD);
        scenario.setEnableDeception(enableDeception);
        scenario.setRepetitions(repetitions);
        scenario.setSeedStrategy(seedStrategy);
        scenario.setBaseSeed(baseSeed);

        EvaluationComparisonResponse response = evaluationFrameworkService.compareSecurity(scenario);

        redirectAttributes.addFlashAttribute("securityForm", scenario);
        redirectAttributes.addFlashAttribute("comparisonResult", response);
        redirectAttributes.addFlashAttribute("notice", "Security comparison completed");
        return "redirect:/simulation/evaluation-compare";
    }

    @PostMapping("/evaluation-compare/defense")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public String runDefenseComparisonFromPage(
            @RequestParam("scenarioName") String scenarioName,
            @RequestParam("mode") EvaluationDefenseCompareMode mode,
            @RequestParam("numNodes") @Min(value = 2, message = "numNodes must be at least 2") @Max(value = 500, message = "numNodes must be at most 500") int numNodes,
            @RequestParam("numEdges") @Min(value = 0, message = "numEdges cannot be negative") @Max(value = 100000, message = "numEdges is too large") int numEdges,
            @RequestParam("attackBudget") @Min(value = 0, message = "attackBudget cannot be negative") @Max(value = 10000, message = "attackBudget is too large") int attackBudget,
            @RequestParam("defenseBudget") @Min(value = 0, message = "defenseBudget cannot be negative") @Max(value = 10000, message = "defenseBudget is too large") int defenseBudget,
            @RequestParam("recoveryBudget") @Min(value = 0, message = "recoveryBudget cannot be negative") @Max(value = 10000, message = "recoveryBudget is too large") int recoveryBudget,
            @RequestParam("rounds") @Min(value = 1, message = "rounds must be at least 1") @Max(value = 100, message = "rounds must be at most 100") int rounds,
            @RequestParam("algorithmType") AlgorithmType algorithmType,
            @RequestParam(value = "enableMTD", defaultValue = "false") boolean enableMTD,
            @RequestParam(value = "enableDeception", defaultValue = "false") boolean enableDeception,
            @RequestParam("repetitions") @Min(value = 1, message = "repetitions must be at least 1") @Max(value = 200, message = "repetitions must be at most 200") int repetitions,
            @RequestParam("seedStrategy") EvaluationSeedStrategy seedStrategy,
            @RequestParam(value = "baseSeed", required = false) Long baseSeed,
            @RequestParam(value = "sweepStart", required = false) Integer sweepStart,
            @RequestParam(value = "sweepEnd", required = false) Integer sweepEnd,
            @RequestParam(value = "sweepStep", required = false) Integer sweepStep,
            RedirectAttributes redirectAttributes
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

        EvaluationComparisonResponse response = evaluationFrameworkService.compareDefense(
                scenario,
                mode,
                sweepStart,
                sweepEnd,
                sweepStep
        );

        redirectAttributes.addFlashAttribute("securityForm", scenario);
        redirectAttributes.addFlashAttribute("defenseMode", mode);
        redirectAttributes.addFlashAttribute("sweepStart", sweepStart);
        redirectAttributes.addFlashAttribute("sweepEnd", sweepEnd);
        redirectAttributes.addFlashAttribute("sweepStep", sweepStep);
        redirectAttributes.addFlashAttribute("comparisonResult", response);
        redirectAttributes.addFlashAttribute("notice", "Defense comparison completed");
        return "redirect:/simulation/evaluation-compare";
    }

    private SimulationRunRequest ensureDashboardForm(Model model) {
        if (model.containsAttribute("form")) {
            return (SimulationRunRequest) model.asMap().get("form");
        }

        SimulationRunRequest form = new SimulationRunRequest();
        form.setNumNodes(DEFAULT_NUM_NODES);
        form.setNumEdges(DEFAULT_NUM_EDGES);
        form.setAttackBudget(DEFAULT_ATTACK_BUDGET);
        form.setDefenseBudget(DEFAULT_DEFENSE_BUDGET);
        form.setRecoveryBudget(DEFAULT_RECOVERY_BUDGET);
        form.setAlgorithmType(AlgorithmType.NORMAL);
        model.addAttribute("form", form);
        return form;
    }

    private int safeInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    public static class CompareForm {
        private int numNodes;
        private int numEdges;
        private int attackBudget;
        private int defenseBudget;
        private int recoveryBudget;

        public int getNumNodes() {
            return numNodes;
        }

        public void setNumNodes(int numNodes) {
            this.numNodes = numNodes;
        }

        public int getNumEdges() {
            return numEdges;
        }

        public void setNumEdges(int numEdges) {
            this.numEdges = numEdges;
        }

        public int getAttackBudget() {
            return attackBudget;
        }

        public void setAttackBudget(int attackBudget) {
            this.attackBudget = attackBudget;
        }

        public int getDefenseBudget() {
            return defenseBudget;
        }

        public void setDefenseBudget(int defenseBudget) {
            this.defenseBudget = defenseBudget;
        }

        public int getRecoveryBudget() {
            return recoveryBudget;
        }

        public void setRecoveryBudget(int recoveryBudget) {
            this.recoveryBudget = recoveryBudget;
        }
    }
}
