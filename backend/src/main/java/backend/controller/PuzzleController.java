package backend.controller;

import backend.dto.ApiSuccessResponse;
import backend.dto.PuzzleChallengeResponse;
import backend.dto.PuzzleSolveRequest;
import backend.dto.PuzzleSolveResponse;
import backend.service.MessagePuzzleService;
import backend.util.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/puzzle")
@Validated
public class PuzzleController {

    private final MessagePuzzleService messagePuzzleService;

    public PuzzleController(MessagePuzzleService messagePuzzleService) {
        this.messagePuzzleService = messagePuzzleService;
    }

    @GetMapping("/{messageId}")
    @PreAuthorize("hasRole('RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<PuzzleChallengeResponse>> getChallenge(
            @PathVariable("messageId") Long messageId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        PuzzleChallengeResponse response = messagePuzzleService.getChallenge(messageId, authentication.getName());
        return ResponseEntity.ok(ApiResponseUtil.success("Puzzle challenge fetched", httpRequest.getRequestURI(), response));
    }

    @PostMapping("/solve/{messageId}")
    @PreAuthorize("hasRole('RECEIVER')")
    public ResponseEntity<ApiSuccessResponse<PuzzleSolveResponse>> solve(
            @PathVariable("messageId") Long messageId,
            @Valid @RequestBody PuzzleSolveRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        PuzzleSolveResponse response = messagePuzzleService.solve(messageId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponseUtil.success("Puzzle solve attempted", httpRequest.getRequestURI(), response));
    }
}

