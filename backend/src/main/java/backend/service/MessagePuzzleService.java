package backend.service;

import backend.adaptive.UserBehaviorProfileService;
import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.config.PuzzleProperties;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessagePuzzleService {

    private final PuzzleRepository puzzleRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final PuzzleService puzzleService;
    private final PuzzleProperties puzzleProperties;
    private final UserBehaviorProfileService userBehaviorProfileService;
    private final AuditService auditService;
    private final Validator validator;

    public MessagePuzzleService(
            PuzzleRepository puzzleRepository,
            MessageRepository messageRepository,
            UserService userService,
            PuzzleService puzzleService,
            PuzzleProperties puzzleProperties,
            UserBehaviorProfileService userBehaviorProfileService,
            AuditService auditService,
            Validator validator
    ) {
        this.puzzleRepository = puzzleRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.puzzleService = puzzleService;
        this.puzzleProperties = puzzleProperties;
        this.userBehaviorProfileService = userBehaviorProfileService;
        this.auditService = auditService;
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

        PuzzleChallengeResponse response = new PuzzleChallengeResponse();
        response.setMessageId(messageId);
        response.setPuzzleType(puzzle.getPuzzleType());
        response.setChallenge(puzzle.getChallenge());
        response.setTargetHash(puzzle.getTargetHash());
        response.setMaxIterations(puzzle.getMaxIterations());
        response.setAttemptsAllowed(puzzle.getAttemptsAllowed());
        response.setAttemptsUsed(puzzle.getAttemptsUsed());
        response.setExpiresAt(puzzle.getExpiresAt());
        response.setSolved(puzzle.getSolvedAt() != null);

        response.setQuestion(buildQuestion(puzzle));
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
            PuzzleSolveResponse response = new PuzzleSolveResponse();
            response.setMessageId(messageId);
            response.setSolved(true);
            response.setAttemptsAllowed(puzzle.getAttemptsAllowed());
            response.setAttemptsUsed(puzzle.getAttemptsUsed());
            response.setSolvedAt(puzzle.getSolvedAt());
            response.setStatus("Puzzle already solved");
            return response;
        }

        if (LocalDateTime.now().isAfter(puzzle.getExpiresAt())) {
            throw new BadRequestException("Puzzle expired");
        }
        if (puzzle.getAttemptsUsed() >= puzzle.getAttemptsAllowed()) {
            throw new BadRequestException("Puzzle attempts exhausted");
        }

        // Consume an attempt, then verify.
        puzzle.setAttemptsUsed(puzzle.getAttemptsUsed() + 1);

        try {
            int nonce = request.getNonce();
            if (nonce >= puzzle.getMaxIterations()) {
                userBehaviorProfileService.recordPuzzleFailure(receiver);
                puzzleRepository.save(puzzle);
                throw new BadRequestException("nonce must be less than maxIterations");
            }
            // Verifies correctness by matching SHA256(challenge:nonce) to targetHash.
            puzzleService.recoverKeyFromNonce(puzzle.getChallenge(), puzzle.getTargetHash(), nonce, puzzle.getWrappedKey());

            LocalDateTime solvedAt = LocalDateTime.now();
            puzzle.setSolvedAt(solvedAt);
            puzzle.setSolvedNonce(nonce);
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
            // already accounted for above (nonce out of range).
            throw ex;
        } catch (RuntimeException ex) {
            userBehaviorProfileService.recordPuzzleFailure(receiver);
            auditService.record(
                    AuditEventType.PUZZLE_SOLVE_FAILURE,
                    receiver.getUsername(),
                    receiver.getUsername(),
                    null,
                    null,
                    null,
                    Map.of(
                            "messageId", message.getId(),
                            "attemptsUsed", puzzle.getAttemptsUsed(),
                            "reason", ex.getMessage()
                    )
            );
            // Auto-hold the message after exhausting attempts so the recovery state machine can take over.
            if (puzzle.getAttemptsUsed() >= puzzle.getAttemptsAllowed() && message.getStatus() != MessageStatus.HELD) {
                message.setStatus(MessageStatus.HELD);
                message.setHoldReason("PUZZLE_ATTEMPTS_EXHAUSTED");
                messageRepository.save(message);
            }
            puzzleRepository.save(puzzle);
            throw ex;
        }
    }

    @Transactional
    public Puzzle buildPuzzleEntity(Message message, String challenge, String targetHash, int maxIterations, String wrappedKeyBase64) {
        return buildPuzzleEntity(message, challenge, targetHash, maxIterations, wrappedKeyBase64, puzzleProperties.getAttemptsAllowed(), puzzleProperties.getTimeLimitSeconds());
    }

    public Puzzle buildPuzzleEntity(
            Message message,
            String challenge,
            String targetHash,
            int maxIterations,
            String wrappedKeyBase64,
            int attemptsAllowed,
            int timeLimitSeconds
    ) {
        Puzzle puzzle = new Puzzle();
        puzzle.setMessage(message);
        puzzle.setPuzzleType(PuzzleType.POW_SHA256);
        puzzle.setChallenge(challenge);
        puzzle.setTargetHash(targetHash);
        puzzle.setMaxIterations(maxIterations);
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

    private String buildQuestion(Puzzle puzzle) {
        return "Find a nonce n in [0, " + puzzle.getMaxIterations() + ") such that SHA-256(challenge + ':' + n) equals targetHash.";
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

