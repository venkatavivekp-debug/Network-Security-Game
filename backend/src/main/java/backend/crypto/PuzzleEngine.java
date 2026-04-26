package backend.crypto;

import backend.adaptive.PuzzleDifficulty;
import backend.dto.PuzzleSolveRequest;
import backend.model.Puzzle;
import backend.model.PuzzleType;

import java.util.Map;

/**
 * Strategy for generating and validating CPHS puzzles. Each puzzle type
 * implements this interface so that {@link backend.service.CPHSService} and
 * {@link backend.service.MessagePuzzleService} can dispatch by
 * {@link PuzzleType} without knowing the details.
 */
public interface PuzzleEngine {

    /** The puzzle type this engine handles. */
    PuzzleType type();

    /**
     * Generate a fresh puzzle that gates the supplied AES message key. The
     * returned {@link GeneratedPuzzle#wrappedKey} must unwrap back to
     * {@code messageKey} only when the correct answer is provided.
     */
    GeneratedPuzzle generate(byte[] messageKey, PuzzleDifficulty difficulty);

    /**
     * Validate a solve attempt. Implementations must throw
     * {@link backend.exception.BadRequestException} on incorrect input. On
     * success they return the recovered AES key plus any audit material.
     */
    SolveResult solve(Puzzle puzzle, PuzzleSolveRequest request);

    /** Human-readable text shown to the receiver in the unlock UI. */
    String questionText(Puzzle puzzle);

    /**
     * Output of {@link #generate}. {@code maxIterations} is only meaningful for
     * the POW puzzle type; other types should pass {@code 0}.
     */
    record GeneratedPuzzle(
            String challenge,
            String targetHash,
            int maxIterations,
            String wrappedKey,
            Map<String, Object> metadata
    ) {}

    /** Output of {@link #solve}. {@code solvedNonce} is set only for POW. */
    record SolveResult(
            byte[] recoveredKey,
            Integer solvedNonce,
            String solvedAnswerHash
    ) {}
}
