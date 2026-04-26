package backend.controller;

import backend.dto.MessageDecryptResponse;
import backend.dto.MessageSendRequest;
import backend.dto.MessageSendResponse;
import backend.dto.MessageSummaryResponse;
import backend.dto.ApiSuccessResponse;
import backend.service.MessageService;
import backend.util.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/message")
@Validated
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('SENDER')")
    public ResponseEntity<ApiSuccessResponse<MessageSendResponse>> sendMessage(
            @Valid @RequestBody MessageSendRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        MessageSendResponse response = messageService.sendMessage(authentication.getName(), request, httpRequest);
        return ResponseEntity.ok(ApiResponseUtil.success("Message sent successfully", httpRequest.getRequestURI(), response));
    }

    @GetMapping("/received")
    @PreAuthorize("hasRole('RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<List<MessageSummaryResponse>>> received(Authentication authentication, HttpServletRequest httpRequest) {
        List<MessageSummaryResponse> response = messageService.getReceivedMessages(authentication.getName());
        return ResponseEntity.ok(ApiResponseUtil.success("Received messages fetched", httpRequest.getRequestURI(), response));
    }

    @PostMapping("/decrypt/{id}")
    @PreAuthorize("hasRole('RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<MessageDecryptResponse>> decrypt(
            @PathVariable("id") @Positive(message = "id must be positive") Long messageId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        MessageDecryptResponse response = messageService.decryptMessage(messageId, authentication.getName());
        return ResponseEntity.ok(ApiResponseUtil.success("Message decrypted successfully", httpRequest.getRequestURI(), response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SENDER','RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<MessageSummaryResponse>> getById(
            @PathVariable("id") @Positive(message = "id must be positive") Long messageId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        // Participant-scoped lookup ensures cross-receiver / cross-sender ids
        // surface the same NotFound shape as a missing id, never leaking
        // existence to outsiders.
        MessageSummaryResponse response = messageService.getSummaryForParticipant(messageId, authentication.getName());
        return ResponseEntity.ok(ApiResponseUtil.success("Message fetched", httpRequest.getRequestURI(), response));
    }
}
