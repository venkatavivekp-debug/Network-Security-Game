package backend.controller;

import backend.dto.ApiSuccessResponse;
import backend.exception.BadRequestException;
import backend.model.EvaluationMetrics;
import backend.service.EvaluationExperimentService;
import backend.util.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/evaluation")
@Validated
public class EvaluationExperimentController {

    private final EvaluationExperimentService evaluationExperimentService;

    public EvaluationExperimentController(EvaluationExperimentService evaluationExperimentService) {
        this.evaluationExperimentService = evaluationExperimentService;
    }

    @GetMapping("/compare")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER','ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Map<String, EvaluationMetrics>>> compare(
            @RequestParam("attackIntensity") @Min(0) @Max(1) double attackIntensity,
            @RequestParam(value = "numberOfRuns", defaultValue = "30") @Min(1) @Max(1000) int numberOfRuns,
            @RequestParam(value = "defenseStrategy", defaultValue = "REDUNDANCY") EvaluationExperimentService.DefenseStrategy defenseStrategy,
            @RequestParam(value = "numNodes", required = false) Integer numNodes,
            @RequestParam(value = "numEdges", required = false) Integer numEdges,
            @RequestParam(value = "seed", required = false) Long seed,
            @RequestParam(value = "persist", defaultValue = "false") boolean persist,
            HttpServletRequest httpRequest
    ) {
        if (attackIntensity < 0 || attackIntensity > 1) {
            throw new BadRequestException("attackIntensity must be between 0 and 1");
        }
        Map<String, EvaluationMetrics> metrics = evaluationExperimentService.compare(
                attackIntensity,
                numberOfRuns,
                defenseStrategy,
                numNodes,
                numEdges,
                seed,
                persist
        );
        return ResponseEntity.ok(ApiResponseUtil.success("Evaluation comparison completed", httpRequest.getRequestURI(), metrics));
    }
}

