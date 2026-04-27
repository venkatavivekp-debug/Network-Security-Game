package backend.service;

import backend.adaptive.ThreatSignalService;
import backend.adaptive.UserBehaviorProfileService;
import backend.audit.AuditService;
import backend.config.PuzzleProperties;
import backend.crypto.PuzzleEngine;
import backend.crypto.PuzzleEngineRegistry;
import backend.dto.PuzzleSolveRequest;
import backend.exception.BadRequestException;
import backend.model.AlgorithmType;
import backend.model.Message;
import backend.model.MessageStatus;
import backend.model.Puzzle;
import backend.model.PuzzleType;
import backend.model.Role;
import backend.model.User;
import backend.repository.MessageRepository;
import backend.repository.PuzzleRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Attack-style test: repeated failures should trigger earlier HOLD (brute force suspicion)
 * even if attempts remain.
 */
class PuzzleBruteForceEscalationTest {

    @Test
    void sustainedFailureBurstTriggersEarlyHold() {
        MessageRepository messageRepo = mock(MessageRepository.class);
        PuzzleRepository puzzleRepo = mock(PuzzleRepository.class);
        UserService userService = mock(UserService.class);
        PuzzleEngineRegistry registry = mock(PuzzleEngineRegistry.class);
        UserBehaviorProfileService behavior = mock(UserBehaviorProfileService.class);
        AuditService audit = mock(AuditService.class);
        ThreatSignalService threat = new ThreatSignalService();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        PuzzleProperties props = new PuzzleProperties();
        props.setAttemptsAllowed(3);
        props.setTimeLimitSeconds(300);
        props.setMaxIterations(180000);

        MessagePuzzleService svc = new MessagePuzzleService(
                puzzleRepo,
                messageRepo,
                userService,
                registry,
                props,
                behavior,
                audit,
                threat,
                validator
        );

        User receiver = new User("bob", "x", Role.RECEIVER);
        receiver.setId(9L);
        when(userService.getRequiredByUsername("bob")).thenReturn(receiver);

        Message msg = new Message();
        msg.setId(42L);
        msg.setReceiver(receiver);
        msg.setAlgorithmType(AlgorithmType.CPHS);
        msg.setStatus(MessageStatus.LOCKED);
        when(messageRepo.findByIdAndReceiver(42L, receiver)).thenReturn(Optional.of(msg));

        Puzzle puzzle = new Puzzle();
        puzzle.setMessage(msg);
        puzzle.setPuzzleType(PuzzleType.ARITHMETIC);
        puzzle.setChallenge("c");
        puzzle.setAttemptsAllowed(3);
        puzzle.setAttemptsUsed(0);
        puzzle.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(puzzleRepo.findByMessageId(42L)).thenReturn(Optional.of(puzzle));

        PuzzleEngine engine = mock(PuzzleEngine.class);
        when(registry.forType(PuzzleType.ARITHMETIC)).thenReturn(engine);
        when(engine.solve(eq(puzzle), any(PuzzleSolveRequest.class))).thenThrow(new BadRequestException("Incorrect answer"));

        when(behavior.recordPuzzleFailure(receiver)).thenReturn(null);
        when(behavior.recentFailureBurst(receiver)).thenReturn(5);

        PuzzleSolveRequest req = new PuzzleSolveRequest();
        req.setAnswer("123");

        assertThrows(BadRequestException.class, () -> svc.solve(42L, req, "bob"));

        assertEquals(MessageStatus.HELD, msg.getStatus());
        assertEquals("SUSPICIOUS_BRUTE_FORCE", msg.getHoldReason());
        verify(messageRepo).save(msg);
    }
}

