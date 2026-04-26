package backend.crypto;

import backend.adaptive.PuzzleDifficulty;
import backend.dto.PuzzleSolveRequest;
import backend.exception.BadRequestException;
import backend.model.Puzzle;
import backend.model.PuzzleType;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;

/**
 * Pattern-recognition puzzle. The challenge is a short numeric sequence (4
 * shown values) that follows one of three rules: arithmetic progression,
 * geometric progression, or Fibonacci-style addition. The receiver submits the
 * next value as an integer.
 */
@Component
public class PatternPuzzleEngine implements PuzzleEngine {

    private final AnswerKeyDerivation derivation;
    private final SecureRandom random = new SecureRandom();

    public PatternPuzzleEngine(AnswerKeyDerivation derivation) {
        this.derivation = derivation;
    }

    @Override
    public PuzzleType type() {
        return PuzzleType.PATTERN;
    }

    @Override
    public GeneratedPuzzle generate(byte[] messageKey, PuzzleDifficulty difficulty) {
        int rule = random.nextInt(3);
        long[] sequence = new long[5];
        switch (rule) {
            case 0 -> {
                long start = 1 + random.nextInt(20);
                long step = 1 + random.nextInt(9);
                for (int i = 0; i < sequence.length; i++) {
                    sequence[i] = start + (long) i * step;
                }
            }
            case 1 -> {
                long start = 1 + random.nextInt(5);
                long ratio = 2 + random.nextInt(2);
                sequence[0] = start;
                for (int i = 1; i < sequence.length; i++) {
                    sequence[i] = sequence[i - 1] * ratio;
                }
            }
            default -> {
                sequence[0] = 1 + random.nextInt(5);
                sequence[1] = 1 + random.nextInt(5);
                for (int i = 2; i < sequence.length; i++) {
                    sequence[i] = sequence[i - 1] + sequence[i - 2];
                }
            }
        }
        StringBuilder challenge = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) challenge.append(", ");
            challenge.append(sequence[i]);
        }
        challenge.append(", ?");

        String canonical = Long.toString(sequence[4]);
        String targetHash = derivation.targetHash(challenge.toString(), canonical);
        String wrappedKey = derivation.wrapKey(challenge.toString(), canonical, messageKey);

        return new GeneratedPuzzle(
                challenge.toString(),
                targetHash,
                0,
                wrappedKey,
                Map.of(
                        "puzzleType", PuzzleType.PATTERN.name(),
                        "challenge", challenge.toString(),
                        "targetHash", targetHash,
                        "wrappedKey", wrappedKey
                )
        );
    }

    @Override
    public SolveResult solve(Puzzle puzzle, PuzzleSolveRequest request) {
        String raw = request == null ? null : request.getAnswer();
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("answer is required for the pattern puzzle");
        }
        String canonical = raw.trim();
        if (!canonical.matches("-?\\d+")) {
            throw new BadRequestException("answer must be an integer");
        }
        if (!derivation.targetHash(puzzle.getChallenge(), canonical).equals(puzzle.getTargetHash())) {
            throw new BadRequestException("Incorrect answer");
        }
        byte[] recovered = derivation.unwrapKey(puzzle.getChallenge(), canonical, puzzle.getWrappedKey());
        return new SolveResult(recovered, null, derivation.answerHash(canonical));
    }

    @Override
    public String questionText(Puzzle puzzle) {
        return "Identify the pattern and submit the next value: " + puzzle.getChallenge();
    }
}
