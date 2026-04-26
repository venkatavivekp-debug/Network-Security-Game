package backend.crypto;

import backend.adaptive.PuzzleDifficulty;
import backend.config.PuzzleProperties;
import backend.dto.PuzzleSolveRequest;
import backend.exception.BadRequestException;
import backend.model.Puzzle;
import backend.model.PuzzleType;
import backend.service.PuzzleService;
import backend.util.EncryptionUtil;
import backend.util.HashUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that every puzzle type the system advertises can:
 * <ul>
 *   <li>be generated and solved with the correct answer (key recovers)</li>
 *   <li>reject a wrong answer with a clear {@link BadRequestException}</li>
 * </ul>
 *
 * <p>Limit/expiry behaviour is enforced one layer up by
 * {@link backend.service.MessagePuzzleService}; that path is exercised
 * separately through the access-control tests.
 */
class PuzzleEnginesTest {

    @Test
    void arithmeticEngineSolvesCorrectlyAndRejectsWrong() {
        AnswerKeyDerivation derivation = derivation();
        ArithmeticPuzzleEngine engine = new ArithmeticPuzzleEngine(derivation);
        byte[] key = repeat((byte) 0xAB, 32);

        PuzzleEngine.GeneratedPuzzle generated = engine.generate(key, new PuzzleDifficulty(30_000, 3, 240));
        Puzzle puzzle = puzzleFor(generated, PuzzleType.ARITHMETIC);

        long expected = computeExpectedAnswer(generated.challenge());
        PuzzleSolveRequest correct = new PuzzleSolveRequest();
        correct.setAnswer(Long.toString(expected));
        PuzzleEngine.SolveResult ok = engine.solve(puzzle, correct);
        assertArrayEquals(key, ok.recoveredKey());
        assertNotNull(ok.solvedAnswerHash());

        PuzzleSolveRequest wrong = new PuzzleSolveRequest();
        wrong.setAnswer(Long.toString(expected + 1));
        BadRequestException ex = assertThrows(BadRequestException.class, () -> engine.solve(puzzle, wrong));
        assertEquals("Incorrect answer", ex.getMessage());
    }

    @Test
    void encodedEngineSolvesCorrectlyAndRejectsWrong() {
        AnswerKeyDerivation derivation = derivation();
        EncodedPuzzleEngine engine = new EncodedPuzzleEngine(derivation);
        byte[] key = repeat((byte) 0x5A, 32);

        PuzzleEngine.GeneratedPuzzle generated = engine.generate(key, new PuzzleDifficulty(30_000, 3, 240));
        Puzzle puzzle = puzzleFor(generated, PuzzleType.ENCODED);
        String decoded = new String(java.util.Base64.getDecoder().decode(generated.challenge()));

        PuzzleSolveRequest correct = new PuzzleSolveRequest();
        correct.setAnswer(decoded);
        PuzzleEngine.SolveResult ok = engine.solve(puzzle, correct);
        assertArrayEquals(key, ok.recoveredKey());

        PuzzleSolveRequest wrong = new PuzzleSolveRequest();
        wrong.setAnswer("not the phrase");
        assertThrows(BadRequestException.class, () -> engine.solve(puzzle, wrong));
    }

    @Test
    void patternEngineSolvesCorrectlyAndRejectsWrong() {
        AnswerKeyDerivation derivation = derivation();
        PatternPuzzleEngine engine = new PatternPuzzleEngine(derivation);
        byte[] key = repeat((byte) 0x3C, 32);

        PuzzleEngine.GeneratedPuzzle generated = engine.generate(key, new PuzzleDifficulty(30_000, 3, 240));
        Puzzle puzzle = puzzleFor(generated, PuzzleType.PATTERN);
        String correctAnswer = inferPatternAnswer(generated.challenge());

        PuzzleSolveRequest correct = new PuzzleSolveRequest();
        correct.setAnswer(correctAnswer);
        PuzzleEngine.SolveResult ok = engine.solve(puzzle, correct);
        assertArrayEquals(key, ok.recoveredKey());

        PuzzleSolveRequest wrong = new PuzzleSolveRequest();
        wrong.setAnswer("0");
        assertThrows(BadRequestException.class, () -> engine.solve(puzzle, wrong));
    }

    @Test
    void powEngineSolvesCorrectlyAndRejectsWrongNonce() {
        PuzzleProperties props = new PuzzleProperties();
        props.setChallengeBytes(8);
        props.setMaxIterations(5_000);
        props.setSimulatedDelayMs(0);
        props.setKeyDerivationSalt("phase4-test-salt");

        HashUtil hashUtil = new HashUtil();
        EncryptionUtil encUtil = new EncryptionUtil();
        encUtil.warmup();
        PuzzleService puzzleService = new PuzzleService(props, hashUtil, encUtil);
        Sha256ProofOfWorkPuzzleEngine engine = new Sha256ProofOfWorkPuzzleEngine(puzzleService, props, hashUtil);

        byte[] key = repeat((byte) 0x7E, 32);
        PuzzleEngine.GeneratedPuzzle generated = engine.generate(key, new PuzzleDifficulty(5_000, 3, 240));
        Puzzle puzzle = puzzleFor(generated, PuzzleType.POW_SHA256);
        puzzle.setMaxIterations(generated.maxIterations());

        PuzzleService.PuzzleSolveResult solved = puzzleService.solveAndRecoverKey(
                generated.challenge(), generated.targetHash(),
                generated.maxIterations(), generated.wrappedKey());
        PuzzleSolveRequest correct = new PuzzleSolveRequest();
        correct.setNonce(solved.getSolvedNonce());
        PuzzleEngine.SolveResult ok = engine.solve(puzzle, correct);
        assertArrayEquals(key, ok.recoveredKey());
        assertEquals(solved.getSolvedNonce(), ok.solvedNonce());

        PuzzleSolveRequest wrong = new PuzzleSolveRequest();
        wrong.setNonce((solved.getSolvedNonce() + 1) % generated.maxIterations());
        assertThrows(BadRequestException.class, () -> engine.solve(puzzle, wrong));
    }

    private static AnswerKeyDerivation derivation() {
        PuzzleProperties props = new PuzzleProperties();
        props.setKeyDerivationSalt("test-salt");
        return new AnswerKeyDerivation(new HashUtil(), props);
    }

    private static Puzzle puzzleFor(PuzzleEngine.GeneratedPuzzle generated, PuzzleType type) {
        Puzzle puzzle = new Puzzle();
        puzzle.setPuzzleType(type);
        puzzle.setChallenge(generated.challenge());
        puzzle.setTargetHash(generated.targetHash());
        puzzle.setWrappedKey(generated.wrappedKey());
        puzzle.setMaxIterations(generated.maxIterations());
        puzzle.setAttemptsAllowed(3);
        puzzle.setAttemptsUsed(0);
        return puzzle;
    }

    private static byte[] repeat(byte v, int n) {
        byte[] out = new byte[n];
        java.util.Arrays.fill(out, v);
        return out;
    }

    /** Re-evaluate the arithmetic puzzle expression so the test does not depend on engine internals. */
    private static long computeExpectedAnswer(String challenge) {
        String[] tokens = challenge.split(" ");
        long[] operands = new long[(tokens.length + 1) / 2];
        char[] operators = new char[tokens.length / 2];
        int oi = 0;
        int opi = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (i % 2 == 0) {
                operands[oi++] = Long.parseLong(tokens[i]);
            } else {
                operators[opi++] = tokens[i].charAt(0);
            }
        }
        return ArithmeticPuzzleEngine.evaluate(operands, operators);
    }

    /** Recompute the next term of the four-term sequence shown in the challenge. */
    private static String inferPatternAnswer(String challenge) {
        String trimmed = challenge.replace("?", "").trim();
        String[] parts = trimmed.split(",");
        long[] vals = new long[4];
        for (int i = 0; i < 4; i++) {
            vals[i] = Long.parseLong(parts[i].trim());
        }
        // Try arithmetic: constant difference
        long d1 = vals[1] - vals[0];
        if (vals[2] - vals[1] == d1 && vals[3] - vals[2] == d1) {
            return Long.toString(vals[3] + d1);
        }
        // Try geometric: constant ratio
        if (vals[0] != 0 && vals[1] % vals[0] == 0) {
            long r = vals[1] / vals[0];
            if (vals[2] == vals[1] * r && vals[3] == vals[2] * r) {
                return Long.toString(vals[3] * r);
            }
        }
        // Fibonacci-style fallback
        return Long.toString(vals[3] + vals[2]);
    }

    @SuppressWarnings("unused")
    private static Map<String, Object> ignored() {
        return Map.of();
    }
}
