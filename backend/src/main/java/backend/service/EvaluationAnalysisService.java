package backend.service;

import backend.audit.AuditEvent;
import backend.audit.AuditEventRepository;
import backend.audit.AuditEventType;
import backend.dto.EvaluationAnalysisResponse;
import backend.dto.EvaluationModeComparisonSummary;
import backend.model.EvaluationMetrics;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns measured evaluation metrics into short, conditional interpretations.
 * Statements are only emitted when the underlying numbers support them.
 */
@Service
public class EvaluationAnalysisService {

    private static final double HIGH_INTENSITY = 2.0 / 3.0;

    private final EvaluationExperimentService evaluationExperimentService;
    private final AuditEventRepository auditEventRepository;

    public EvaluationAnalysisService(
            EvaluationExperimentService evaluationExperimentService,
            AuditEventRepository auditEventRepository
    ) {
        this.evaluationExperimentService = evaluationExperimentService;
        this.auditEventRepository = auditEventRepository;
    }

    public EvaluationAnalysisResponse analyze(
            double attackIntensity,
            int numberOfRuns,
            EvaluationExperimentService.DefenseStrategy defenseStrategy,
            Integer numNodes,
            Integer numEdges,
            Long seed,
            boolean persist
    ) {
        Map<String, EvaluationMetrics> metrics = evaluationExperimentService.compare(
                attackIntensity,
                numberOfRuns,
                defenseStrategy,
                numNodes,
                numEdges,
                seed,
                persist
        );

        EvaluationModeComparisonSummary comparison =
                evaluationExperimentService.buildComparisonSummary(attackIntensity, metrics);
        String bestMode = comparison.getBestModeUnderAttackIntensity();

        Map<String, String> recommended = new LinkedHashMap<>();
        recommended.put("LOW", pickBestForIntensity(0.2, numberOfRuns, defenseStrategy, numNodes, numEdges, seed));
        recommended.put("MEDIUM", pickBestForIntensity(0.5, numberOfRuns, defenseStrategy, numNodes, numEdges, seed));
        recommended.put("HIGH", pickBestForIntensity(0.85, numberOfRuns, defenseStrategy, numNodes, numEdges, seed));

        List<String> insights = buildInsights(attackIntensity, metrics, comparison, recommended);

        OperationalRates rates = computeOperationalRates();

        EvaluationAnalysisResponse response = new EvaluationAnalysisResponse();
        response.setMetrics(metrics);
        response.setInsights(insights);
        response.setBestMode(bestMode);
        response.setRecommendedModeByThreatLevel(recommended);
        response.setPuzzleFailureEscalationRate(rates.puzzleFailureEscalationRate);
        response.setAdminReviewRate(rates.adminReviewRate);
        return response;
    }

    /**
     * Computes two operational rates from the live audit log:
     * <ul>
     *     <li>puzzleFailureEscalationRate = puzzle failures / (puzzle attempts) over the last
     *     200 audit events. Returns null if there are no puzzle attempts in the window.</li>
     *     <li>adminReviewRate = admin "release_message" or "hold_message" actions / message sends
     *     over the same window. Returns null if there were no message sends.</li>
     * </ul>
     */
    private OperationalRates computeOperationalRates() {
        List<AuditEvent> recent = auditEventRepository.findTop200ByOrderByCreatedAtDesc();
        long puzzleSolves = 0;
        long puzzleFails = 0;
        long messageSends = 0;
        long adminReviews = 0;
        for (AuditEvent e : recent) {
            AuditEventType t = e.getEventType();
            if (t == null) continue;
            switch (t) {
                case PUZZLE_SOLVE_SUCCESS -> puzzleSolves++;
                case PUZZLE_SOLVE_FAILURE -> puzzleFails++;
                case MESSAGE_SEND -> messageSends++;
                case ADMIN_ACTION -> {
                    if (e.getDetails() != null
                            && (e.getDetails().contains("hold_message") || e.getDetails().contains("release_message"))) {
                        adminReviews++;
                    }
                }
                default -> {
                }
            }
        }
        long puzzleAttempts = puzzleSolves + puzzleFails;
        Double escalationRate = puzzleAttempts == 0 ? null : ((double) puzzleFails) / puzzleAttempts;
        Double adminRate = messageSends == 0 ? null : ((double) adminReviews) / messageSends;
        return new OperationalRates(escalationRate, adminRate);
    }

    private record OperationalRates(Double puzzleFailureEscalationRate, Double adminReviewRate) {
    }

    private String pickBestForIntensity(
            double intensity,
            int numberOfRuns,
            EvaluationExperimentService.DefenseStrategy defenseStrategy,
            Integer numNodes,
            Integer numEdges,
            Long seed
    ) {
        Map<String, EvaluationMetrics> band = evaluationExperimentService.compare(
                intensity,
                numberOfRuns,
                defenseStrategy,
                numNodes,
                numEdges,
                seed,
                false
        );
        return evaluationExperimentService.bestModeName(band);
    }

    private List<String> buildInsights(
            double attackIntensity,
            Map<String, EvaluationMetrics> metrics,
            EvaluationModeComparisonSummary comparison,
            Map<String, String> recommended
    ) {
        List<String> insights = new ArrayList<>();
        insights.add(comparison.getSecurityVsEffortTradeOff());

        EvaluationMetrics normal = metrics.get(EvaluationExperimentService.ExperimentMode.NORMAL.name());
        EvaluationMetrics shcs = metrics.get(EvaluationExperimentService.ExperimentMode.SHCS.name());
        EvaluationMetrics cphs = metrics.get(EvaluationExperimentService.ExperimentMode.CPHS.name());
        EvaluationMetrics adaptive = metrics.get(EvaluationExperimentService.ExperimentMode.ADAPTIVE.name());

        if (normal != null && cphs != null && attackIntensity >= HIGH_INTENSITY) {
            if (cphs.getCompromiseRatio() < normal.getCompromiseRatio() - 1e-9) {
                insights.add(
                        "Under high attack intensity in this run, CPHS recorded a lower compromiseRatio than NORMAL "
                                + "(CPHS=" + round3(cphs.getCompromiseRatio()) + ", NORMAL=" + round3(normal.getCompromiseRatio()) + ")."
                );
            } else if (Math.abs(cphs.getCompromiseRatio() - normal.getCompromiseRatio()) < 1e-9) {
                insights.add(
                        "Under high attack intensity in this run, CPHS and NORMAL tied on compromiseRatio ("
                                + round3(normal.getCompromiseRatio()) + ")."
                );
            }
        }

        if (adaptive != null && normal != null) {
            if (adaptive.getResilienceScore() > normal.getResilienceScore() + 1e-9
                    && adaptive.getUserEffortScore() <= normal.getUserEffortScore() + 0.5) {
                insights.add(
                        "Adaptive mode achieved a higher resilienceScore than NORMAL with userEffortScore "
                                + round3(adaptive.getUserEffortScore()) + " versus "
                                + round3(normal.getUserEffortScore()) + "."
                );
            }
        }

        if (adaptive != null && shcs != null && cphs != null) {
            double ae = adaptive.getUserEffortScore();
            double se = shcs.getUserEffortScore();
            double ce = cphs.getUserEffortScore();
            double low = Math.min(se, ce);
            double high = Math.max(se, ce);
            if (adaptive.getResilienceScore() > Math.max(shcs.getResilienceScore(), cphs.getResilienceScore()) + 1e-9
                    && ae + 1e-9 >= low && ae <= high + 1e-9) {
                insights.add(
                        "Adaptive mode shows higher resilienceScore than both SHCS and CPHS here, with userEffortScore "
                                + round3(ae) + " between SHCS (" + round3(se) + ") and CPHS (" + round3(ce) + ")."
                );
            }
        }

        if (normal != null && attackIntensity >= HIGH_INTENSITY) {
            double nComp = normal.getCompromiseRatio();
            boolean normalHighest = metrics.entrySet().stream()
                    .filter(e -> !e.getKey().equals(EvaluationExperimentService.ExperimentMode.NORMAL.name()))
                    .allMatch(e -> e.getValue().getCompromiseRatio() + 1e-9 < nComp);
            if (normalHighest) {
                insights.add(
                        "At this intensity, NORMAL had the highest compromiseRatio among modes ("
                                + round3(nComp) + ")."
                );
            }
        }

        String labelLine = comparison.getBaselineModeLabels().entrySet().stream()
                .map(e -> e.getKey() + "→" + e.getValue())
                .collect(Collectors.joining("; "));
        insights.add("Research category mapping for this harness: " + labelLine);

        insights.add(
                "Heuristic recommendations by modeled threat band (same harness, intensities 0.2 / 0.5 / 0.85): LOW→"
                        + recommended.get("LOW") + ", MEDIUM→" + recommended.get("MEDIUM") + ", HIGH→" + recommended.get("HIGH") + "."
        );

        return insights;
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
