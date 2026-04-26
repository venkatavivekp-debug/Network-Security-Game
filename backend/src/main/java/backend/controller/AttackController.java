package backend.controller;

import backend.dto.AttackSimulationResponse;
import backend.dto.ApiSuccessResponse;
import backend.model.Message;
import backend.security.AccessPolicyService;
import backend.service.AttackSimulationService;
import backend.simulation.AttackSimulationResult;
import backend.util.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attack")
@Validated
public class AttackController {

    private final AttackSimulationService attackSimulationService;
    private final AccessPolicyService accessPolicyService;

    public AttackController(AttackSimulationService attackSimulationService, AccessPolicyService accessPolicyService) {
        this.attackSimulationService = attackSimulationService;
        this.accessPolicyService = accessPolicyService;
    }

    @GetMapping("/simulate/{messageId}")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<AttackSimulationResponse>> simulate(
            @PathVariable @Positive(message = "messageId must be positive") Long messageId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        // Object-level authorization: only the sender or the receiver of this
        // message may run an attack simulation against it. Otherwise an attacker
        // could enumerate message ids and learn algorithm types they are not
        // a participant in.
        Message message = accessPolicyService.requireParticipant(messageId, authentication);
        AttackSimulationResult result = attackSimulationService.simulate(message);

        AttackSimulationResponse response = new AttackSimulationResponse();
        response.setMessageId(message.getId());
        response.setAlgorithmType(message.getAlgorithmType());
        response.setClassificationSuccess(result.isClassificationSuccess());
        response.setClassificationConfidence(result.getClassificationConfidence());
        response.setSelectiveJammingSuccess(result.isSelectiveJammingSuccess());
        response.setTimeRequiredMs(result.getTimeRequiredMs());
        response.setResultSummary(result.getSummary());

        return ResponseEntity.ok(ApiResponseUtil.success("Attack simulation completed", httpRequest.getRequestURI(), response));
    }
}
