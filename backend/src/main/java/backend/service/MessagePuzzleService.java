package backend.service;

import backend.adaptive.ThreatSignalService;
import backend.adaptive.UserBehaviorProfileService;
import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.config.PuzzleProperties;
import backend.crypto.PuzzleEngine;
import backend.crypto.PuzzleEngineRegistry;
import backend.dto.PuzzleChallengeResponse;
import backend.dto.PuzzleSolveRequest;
import backend.dto.PuzzleSolveResponse;
import backend.exception.BadRequestException;
import backend.exception.ResourceNotFoundException;
import backend.model.AlgorithmType;
import backend.model.Message;
import backend.model.MessageStatus;
import backend.model.Puzzle;
import backend.model.PuzzleType;
import backend.model.Role;
import backend.model.User;
import backend.repository.MessageRepository;
import backend.repository.PuzzleRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessagePuzzleService {

    private final PuzzleRepository puzzleRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final PuzzleEngineRegistry puzzleEngineRegistry;
    private final PuzzleProperties puzzleProperties;
    private final UserBehaviorProfileService userBehaviorProfileService;
    private final AuditService auditService;
    private final ThreatSignalService threatSignalService;
    private final Validator validator;

    public MessagePuzzleService(
            PuzzleRepository puzzleRepository,
            MessageRepository messageRepository,
            UserService userService,
            PuzzleEngineRegistry puzzleEngineRegistry,
            PuzzleProperties puzzleProperties,
            UserBehaviorProfileService userBehaviorProfileService,
            AuditService auditService,
            ThreatSignalService threatSignalService,
            Validator validator
    ) {
        this.puzzleRepository = puzzleRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.puzzleEngineRegistry = puzzleEngineRegistry;
        this.puzzleProperties = puzzleProperties;
        this.userBehaviorProfileService = userBehaviorProfileService;
        this.auditService = auditService;
        this.threatSignalService = threatSignalService;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public PuzzleChallengeResponse getChallenge(Long messageId, String receiverUsername) {
        Message message = requireReceiverMessage(messageId, receiverUsername);
        if (message.getAlgorithmType() != AlgorithmType.CPHS) {
            throw new BadRequestException("Puzzle is only required for CPHS messages");
        }

        Puzzle puzzle = puzzleRepository.findByMessageId(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Puzzle not found for message: " + messageId));

        PuzzleEngine engine = puzzleEngineRegistry.forType(puzzle.getPuzzleType());

        PuzzleChallengeResponse response = new PuzzleChallengeResponse();
        response.setMessageId(messageId);
        response.setPuzzleType(puzzle.getPuzzleType());
        response.setChallenge(puzzle.getChallenge());
        // Hash gating material is only meaningful for PoW puzzles. Other types
        // do not need to expose targetHash to the client because verification
        // happens server-side from the submitted answer.
        if (puzzle.getPuzzleType() == PuzzleType.POW_SHA256) {
            response.setTargetHash(puzzle.getTargetHash());
        }
        response.setMaxIterations(puzzle.getMaxIterations());
        response.setAttemptsAllowed(puzzle.getAttemptsAllowed());
        response.setAttemptsUsed(puzzle.getAttemptsUsed());
        response.setExpiresAt(puzzle.getExpiresAt());
        response.setSolved(puzzle.getSolvedAt() != null);
        response.setQuestion(engine.questionText(puzzle));
        return response;
    }

    @Transactional
    public PuzzleSolveResponse solve(Long messageId, PuzzleSolveRequest request, String receiverUsername) {
        validateBean(request);
        Message message = requireReceiverMessage(messageId, receiverUsername);
        User receiver = message.getReceiver();
        if (message.getAlgorithmType() != AlgorithmType.CPHS) {
            throw new BadRequestException("Puzzle is only required for CPHS messages");
        }

        Puzzle puzzle = puzzleRepository.findByMessageId(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Puzzle not found for message: " + messageId));

        if (puzzle.getSolvedAt() != null) {
            return alreadySolvedResponse(messageId, puzzle);
        }

        if (LocalDateTime.now().isAfter(puzzle.getExpiresAt())) {
            throw new BadRequestException("Puzzle expired");
        }
        if (puzzle.getAttemptsUsed() >= puzzle.getAttemptsAllowed()) {
            throw new BadRequestException("Puzzle attempts exhausted");
        }

        puzzle.setAttemptsUsed(puzzle.getAttemptsUsed() + 1);
        PuzzleEngine engine = puzzleEngineRegistry.forType(puzzle.getPuzzleType());

        try {
            PuzzleEngine.SolveResult solveResult = engine.solve(puzzle, request);

            LocalDateTime solvedAt = LocalDateTime.now();
            puzzle.setSolvedAt(solvedAt);
            puzzle.setSolvedNonce(solveResult.solvedNonce());
            puzzle.setSolvedAnswerHash(solveResult.solvedAnswerHash());
            // For non-PoW puzzles we keep the AES key around so subsequent decrypt calls can succeed.
            if (puzzle.getPuzzleType() != PuzzleType.POW_SHA256 && solveResult.recoveredKey() != null) {
                puzzle.setRecoveredKey(Base64.getEncoder().encodeToString(solveResult.recoveredKey()));
            }
            message.setStatus(MessageStatus.UNLOCKED);

            long solveTimeMs = Math.max(0, Duration.between(message.getCreatedAt(), solvedAt).toMillis());
            userBehaviorProfileService.recordPuzzleSuccess(receiver, solveTimeMs);
            auditService.record(
                    AuditEventType.PUZZLE_SOLVE_SUCCESS,
                    receiver.getUsername(),
                    receiver.getUsername(),
                    null,
                    null,
                    null,
                    Map.of(
                            "messageId", message.getId(),
                            "puzzleType", puzzle.getPuzzleType().name(),
                            "attemptsUsed", puzzle.getAttemptsUsed(),
                            "solveTimeMs", solveTimeMs
                    )
            );

            puzzleRepository.save(puzzle);
            messageRepository.save(message);

            PuzzleSolveResponse response = new PuzzleSolveResponse();
            response.setMessageId(messageId);
            response.setSolved(true);
            response.setAttemptsAllowed(puzzle.getAttemptsAllowed());
            response.setAttemptsUsed(puzzle.getAttemptsUsed());
            response.setSolvedAt(puzzle.getSolvedAt());
            response.setStatus("Puzzle solved. Message unlocked.");
            return response;
        } catch (BadRequestException ex) {
            userBehaviorProfileService.recordPuzzleFailure(receiver);
            int burst = userBehaviorProfileService.recentFailureBurst(receiver);
            auditService.record(
                    AuditEventType.PUZZLE_SOLVE_FAILURE,
                    receiver.getUsername(),
                    receiver.getUsername(),
                    null,
                    null,
                    null,
                    Map.of(
                            "messageId", message.getId(),
                            "puzzleType", puzzle.getPuzzleType().name(),
                            "attemptsUsed", puzzle.getAttemptsUsed(),
                            "reason", ex.getMessage(),
                            "failureBurst", burst
                    )
            );
            // Anomaly-driven enforcement: if we see a sustained failure burst, hold early
            // even if attempts remain. This slows brute forcing while keeping a clear
            // recovery path (admin review / reset counters).
            if (burst >= 5 && message.getStatus() != MessageStatus.HELD) {
                message.setStatus(MessageStatus.HELD);
                message.setHoldReason("SUSPICIOUS_BRUTE_FORCE");
                messageRepository.save(message);
                try {
                    auditService.record(
                            AuditEventType.ADAPTIVE_ESCALATION,
                            receiver.getUsername(),
                            receiver.getUsername(),
                            null,
                            null,
                            null,
                            Map.of("decision", "hold_early_on_failure_burst", "failureBurst", burst, "messageId", message.getId())
                    );
                } catch (RuntimeException ignored) {
                }
            }
            if (puzzle.getAttemptsUsed() >= puzzle.getAttemptsAllowed() && message.getStatus() != MessageStatus.HELD) {
                message.setStatus(MessageStatus.HELD);
                message.setHoldReason("PUZZLE_ATTEMPTS_EXHAUSTED");
                messageRepository.save(message);
                // Exhausted attempts contribute to the system-wide attack pressure so the
                // simulation view reacts; we add a small bump that can never push past 1.0.
                double bumped = Math.min(1.0, threatSignalService.currentAttackIntensity01() + 0.05);
                threatSignalService.setAttackIntensity01(bumped);
            }
            puzzleRepository.save(puzzle);
            throw ex;
        }
    }

    private PuzzleSolveResponse alreadySolvedResponse(Long messageId, Puzzle puzzle) {
        PuzzleSolveResponse response = new PuzzleSolveResponse();
        response.setMessageId(messageId);
        response.setSolved(true);
        response.setAttemptsAllowed(puzzle.getAttemptsAllowed());
        response.setAttemptsUsed(puzzle.getAttemptsUsed());
        response.setSolvedAt(puzzle.getSolvedAt());
        response.setStatus("Puzzle already solved");
        return response;
    }

    @Transactional
    public Puzzle buildPuzzleEntity(Message message, String challenge, String targetHash, int maxIterations, String wrappedKeyBase64) {
        return buildPuzzleEntity(
                message,
                PuzzleType.POW_SHA256,
                challenge,
                targetHash,
                maxIterations,
                wrappedKeyBase64,
                puzzleProperties.getAttemptsAllowed(),
                puzzleProperties.getTimeLimitSeconds()
        );
    }

    public Puzzle buildPuzzleEntity(
            Message message,
            PuzzleType puzzleType,
            String challenge,
            String targetHash,
            int maxIterations,
            String wrappedKeyBase64,
            int attemptsAllowed,
            int timeLimitSeconds
    ) {
        Puzzle puzzle = new Puzzle();
        puzzle.setMessage(message);
        puzzle.setPuzzleType(puzzleType == null ? PuzzleType.POW_SHA256 : puzzleType);
        puzzle.setChallenge(challenge);
        puzzle.setTargetHash(targetHash);
        puzzle.setMaxIterations(Math.max(0, maxIterations));
        puzzle.setWrappedKey(wrappedKeyBase64);
        puzzle.setAttemptsAllowed(Math.max(1, Math.min(6, attemptsAllowed)));
        puzzle.setAttemptsUsed(0);
        puzzle.setExpiresAt(LocalDateTime.now().plusSeconds(Math.max(60, Math.min(1800, timeLimitSeconds))));
        return puzzle;
    }

    private Message requireReceiverMessage(Long messageId, String receiverUsername) {
        if (messageId == null || messageId <= 0) {
            throw new BadRequestException("messageId must be positive");
        }
        User receiver = userService.getRequiredByUsername(receiverUsername);
        if (receiver.getRole() != Role.RECEIVER) {
            throw new BadRequestException("Only users with RECEIVER role can access puzzles");
        }
        return messageRepository.findByIdAndReceiver(messageId, receiver)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found for receiver: " + messageId));
    }

    private <T> void validateBean(T bean) {
        Set<ConstraintViolation<T>> violations = validator.validate(bean);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new BadRequestException(message);
        }
    }
}
