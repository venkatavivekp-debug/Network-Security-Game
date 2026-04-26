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
import backend.repository.UserBehaviorProfileRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the per-puzzle limit and expiry handling that lives in
 * {@link MessagePuzzleService} (engine tests cover correct/wrong solves).
 */
class MessagePuzzleLimitsTest {

    @Test
    void expiredPuzzleIsRejected() {
        Fixture f = new Fixture();
        Puzzle puzzle = f.puzzle(PuzzleType.ARITHMETIC, 3, 0, LocalDateTime.now().minusMinutes(1));
        f.attach(puzzle);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> f.svc.solve(7L, attempt("123"), "carol"));
        assertEquals("Puzzle expired", ex.getMessage());
    }

    @Test
    void exhaustedAttemptsAreRejected() {
        Fixture f = new Fixture();
        Puzzle puzzle = f.puzzle(PuzzleType.ARITHMETIC, 2, 2, LocalDateTime.now().plusMinutes(5));
        f.attach(puzzle);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> f.svc.solve(7L, attempt("123"), "carol"));
        assertEquals("Puzzle attempts exhausted", ex.getMessage());
    }

    private static PuzzleSolveRequest attempt(String answer) {
        PuzzleSolveRequest req = new PuzzleSolveRequest();
        req.setAnswer(answer);
        return req;
    }

    private static class Fixture {
        final PuzzleRepository puzzleRepository = mock(PuzzleRepository.class);
        final MessageRepository messageRepository = mock(MessageRepository.class);
        final UserService userService = mock(UserService.class);
        final UserBehaviorProfileRepository behaviorRepository = mock(UserBehaviorProfileRepository.class);
        final UserBehaviorProfileService behaviorService = new UserBehaviorProfileService(behaviorRepository);
        final AuditService auditService = mock(AuditService.class);
        final ThreatSignalService threatSignalService = new ThreatSignalService();
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final PuzzleEngineRegistry registry;
        final MessagePuzzleService svc;
        final User receiver;
        final Message message;

        Fixture() {
            this.registry = new PuzzleEngineRegistry(List.of(
                    stubEngine(PuzzleType.POW_SHA256),
                    stubEngine(PuzzleType.ARITHMETIC),
                    stubEngine(PuzzleType.ENCODED),
                    stubEngine(PuzzleType.PATTERN)
            ));

            this.receiver = new User("carol", "x", Role.RECEIVER);
            this.receiver.setId(99L);
            this.message = new Message();
            this.message.setId(7L);
            this.message.setReceiver(receiver);
            this.message.setStatus(MessageStatus.LOCKED);
            this.message.setAlgorithmType(AlgorithmType.CPHS);
            this.message.setCreatedAt(LocalDateTime.now().minusMinutes(1));

            when(userService.getRequiredByUsername("carol")).thenReturn(receiver);
            when(messageRepository.findByIdAndReceiver(7L, receiver)).thenReturn(Optional.of(message));

            this.svc = new MessagePuzzleService(
                    puzzleRepository,
                    messageRepository,
                    userService,
                    registry,
                    new PuzzleProperties(),
                    behaviorService,
                    auditService,
                    threatSignalService,
                    validator
            );
        }

        Puzzle puzzle(PuzzleType type, int attemptsAllowed, int attemptsUsed, LocalDateTime expiresAt) {
            Puzzle p = new Puzzle();
            p.setMessage(message);
            p.setPuzzleType(type);
            p.setChallenge("1 + 1");
            p.setTargetHash("hash");
            p.setMaxIterations(0);
            p.setWrappedKey("AAAA");
            p.setAttemptsAllowed(attemptsAllowed);
            p.setAttemptsUsed(attemptsUsed);
            p.setExpiresAt(expiresAt);
            return p;
        }

        void attach(Puzzle puzzle) {
            when(puzzleRepository.findByMessageId(7L)).thenReturn(Optional.of(puzzle));
        }

        private static PuzzleEngine stubEngine(PuzzleType type) {
            PuzzleEngine engine = mock(PuzzleEngine.class);
            when(engine.type()).thenReturn(type);
            return engine;
        }
    }
}
