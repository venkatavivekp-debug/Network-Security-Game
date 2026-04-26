package backend.crypto;

import backend.adaptive.PuzzleDifficulty;
import backend.config.PuzzleProperties;
import backend.dto.PuzzleSolveRequest;
import backend.exception.BadRequestException;
import backend.model.Puzzle;
import backend.model.PuzzleType;
import backend.util.HashUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Engine-level tests for the new CPHS puzzle types. Each puzzle must:
 *   1. produce a wrappedKey that round-trips to the original AES message key
 *      when the correct answer is supplied,
 *   2. reject a wrong answer with a {@link BadRequestException},
 *   3. report the correct {@link PuzzleType}.
 */
class PuzzleEngineTest {

    private static AnswerKeyDerivation derivation;
    private static PuzzleProperties properties;

    @BeforeAll
    static void setup() {
        properties = new PuzzleProperties();
        properties.setKeyDerivationSalt("phase4-test-salt");
        properties.setMaxIterations(5_000);
        derivation = new AnswerKeyDerivation(new HashUtil(), properties);
    }

    @Test
    void arithmeticEngineRoundTripsKeyWhenAnswerIsCorrect() {
        ArithmeticPuzzleEngine engine = new ArithmeticPuzzleEngine(derivation);
        byte[] originalKey = key32();

        PuzzleEngine.GeneratedPuzzle generated = engine.generate(originalKey, new PuzzleDifficulty(40_000, 3, 240));
        assertEquals(PuzzleType.ARITHMETIC, engine.type());
        assertNotNull(generated.targetHash());

        long correctAnswer = ArithmeticPuzzleEngine.evaluate(parseOperands(generated.challenge()), parseOperators(generated.challenge()));
        Puzzle puzzle = puzzle(PuzzleType.ARITHMETIC, generated);
        PuzzleSolveRequest request = answerRequest(Long.toString(correctAnswer));

        PuzzleEngine.SolveResult result = engine.solve(puzzle, request);
        assertArrayEquals(originalKey, result.recoveredKey());
        assertNull(result.solvedNonce());
    }

    @Test
    void arithmeticEngineRejectsIncorrectAnswer() {
        ArithmeticPuzzleEngine engine = new ArithmeticPuzzleEngine(derivation);
        PuzzleEngine.GeneratedPuzzle generated = engine.generate(key32(), new PuzzleDifficulty(20_000, 3, 180));
        Puzzle puzzle = puzzle(PuzzleType.ARITHMETIC, generated);
        // 99999 is virtually never the right answer for the small expressions we generate.
        assertThrows(BadRequestException.class, () -> engine.solve(puzzle, answerRequest("99999")));
    }

    @Test
    void encodedEngineRoundTripsKeyForBase64DecodedAnswer() {
        EncodedPuzzleEngine engine = new EncodedPuzzleEngine(derivation);
        byte[] originalKey = key32();
        PuzzleEngine.GeneratedPuzzle generated = engine.generate(originalKey, new PuzzleDifficulty(20_000, 3, 180));
        assertEquals(PuzzleType.ENCODED, engine.type());

        String decoded = new String(java.util.Base64.getDecoder().decode(generated.challenge()));
        Puzzle puzzle = puzzle(PuzzleType.ENCODED, generated);
        PuzzleEngine.SolveResult result = engine.solve(puzzle, answerRequest(decoded.toUpperCase()));
        assertArrayEquals(originalKey, result.recoveredKey());
    }

    @Test
    void encodedEngineRejectsWrongPhrase() {
        EncodedPuzzleEngine engine = new EncodedPuzzleEngine(derivation);
        PuzzleEngine.GeneratedPuzzle generated = engine.generate(key32(), new PuzzleDifficulty(20_000, 3, 180));
        Puzzle puzzle = puzzle(PuzzleType.ENCODED, generated);
        assertThrows(BadRequestException.class, () -> engine.solve(puzzle, answerRequest("not the right phrase")));
    }

    @Test
    void patternEngineRoundTripsKeyForCorrectNextValue() {
        PatternPuzzleEngine engine = new PatternPuzzleEngine(derivation);
        byte[] originalKey = key32();
        PuzzleEngine.GeneratedPuzzle generated = engine.generate(originalKey, new PuzzleDifficulty(20_000, 3, 180));
        assertEquals(PuzzleType.PATTERN, engine.type());

        long expected = inferNext(generated.challenge());
        Puzzle puzzle = puzzle(PuzzleType.PATTERN, generated);
        PuzzleEngine.SolveResult result = engine.solve(puzzle, answerRequest(Long.toString(expected)));
        assertArrayEquals(originalKey, result.recoveredKey());
    }

    @Test
    void patternEngineRejectsBadAnswer() {
        PatternPuzzleEngine engine = new PatternPuzzleEngine(derivation);
        PuzzleEngine.GeneratedPuzzle generated = engine.generate(key32(), new PuzzleDifficulty(20_000, 3, 180));
        Puzzle puzzle = puzzle(PuzzleType.PATTERN, generated);
        assertThrows(BadRequestException.class, () -> engine.solve(puzzle, answerRequest("0")));
    }

    @Test
    void registryReturnsCorrectEngineForEveryType() {
        ArithmeticPuzzleEngine arith = new ArithmeticPuzzleEngine(derivation);
        EncodedPuzzleEngine encoded = new EncodedPuzzleEngine(derivation);
        PatternPuzzleEngine pattern = new PatternPuzzleEngine(derivation);

        Sha256ProofOfWorkPuzzleEngine pow = new Sha256ProofOfWorkPuzzleEngine(
                new backend.service.PuzzleService(properties, new HashUtil(), new backend.util.EncryptionUtil()),
                properties,
                new HashUtil()
        );
        PuzzleEngineRegistry registry = new PuzzleEngineRegistry(java.util.List.of(pow, arith, encoded, pattern));
        assertEquals(arith, registry.forType(PuzzleType.ARITHMETIC));
        assertEquals(encoded, registry.forType(PuzzleType.ENCODED));
        assertEquals(pattern, registry.forType(PuzzleType.PATTERN));
        assertEquals(pow, registry.forType(PuzzleType.POW_SHA256));
    }

    private static byte[] key32() {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) key[i] = (byte) (i * 7 + 3);
        return key;
    }

    private static long[] parseOperands(String expr) {
        String[] parts = expr.split(" ");
        // Format is: N op N op N ...
        long[] operands = new long[(parts.length + 1) / 2];
        int idx = 0;
        for (int i = 0; i < parts.length; i += 2) {
            operands[idx++] = Long.parseLong(parts[i]);
        }
        return Arrays.copyOf(operands, idx);
    }

    private static char[] parseOperators(String expr) {
        String[] parts = expr.split(" ");
        char[] operators = new char[parts.length / 2];
        int idx = 0;
        for (int i = 1; i < parts.length; i += 2) {
            operators[idx++] = parts[i].charAt(0);
        }
        return Arrays.copyOf(operators, idx);
    }

    private static long inferNext(String challenge) {
        // Format: "a, b, c, d, ?"
        String stripped = challenge.replace(", ?", "");
        String[] parts = stripped.split(", ");
        long[] seq = Arrays.stream(parts).mapToLong(Long::parseLong).toArray();
        long d1 = seq[1] - seq[0];
        long d2 = seq[2] - seq[1];
        if (d1 == d2) {
            return seq[3] + d1;
        }
        if (seq[0] != 0 && seq[1] / seq[0] == seq[2] / seq[1]) {
            return seq[3] * (seq[1] / seq[0]);
        }
        return seq[3] + seq[2];
    }

    private static Puzzle puzzle(PuzzleType type, PuzzleEngine.GeneratedPuzzle generated) {
        Puzzle puzzle = new Puzzle();
        puzzle.setPuzzleType(type);
        puzzle.setChallenge(generated.challenge());
        puzzle.setTargetHash(generated.targetHash());
        puzzle.setMaxIterations(generated.maxIterations());
        puzzle.setWrappedKey(generated.wrappedKey());
        puzzle.setAttemptsAllowed(3);
        puzzle.setAttemptsUsed(0);
        return puzzle;
    }

    private static PuzzleSolveRequest answerRequest(String answer) {
        PuzzleSolveRequest request = new PuzzleSolveRequest();
        request.setAnswer(answer);
        return request;
    }
}
