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
 * Arithmetic puzzle. The challenge is a small expression that the receiver must
 * evaluate (e.g. {@code 23 + 45 * 2}). Difficulty modulates the number of
 * operands (2-4) and the operand range. The expected canonical answer is the
 * integer result rendered without spaces, which is hashed and stored in
 * {@code targetHash}.
 *
 * <p>The expression is restricted to {@code +}, {@code -}, {@code *}; division
 * is omitted to avoid non-integer results. Evaluation honours operator
 * precedence (multiplication before addition/subtraction), which the receiver
 * must do too.
 */
@Component
public class ArithmeticPuzzleEngine implements PuzzleEngine {

    private final AnswerKeyDerivation derivation;
    private final SecureRandom random = new SecureRandom();

    public ArithmeticPuzzleEngine(AnswerKeyDerivation derivation) {
        this.derivation = derivation;
    }

    @Override
    public PuzzleType type() {
        return PuzzleType.ARITHMETIC;
    }

    @Override
    public GeneratedPuzzle generate(byte[] messageKey, PuzzleDifficulty difficulty) {
        int iterations = difficulty == null ? 30_000 : difficulty.getMaxIterations();
        int operandCount = clamp(2 + (iterations / 30_000), 2, 4);
        int operandMax = clamp(20 + iterations / 5_000, 25, 99);

        StringBuilder expression = new StringBuilder();
        long[] operands = new long[operandCount];
        char[] operators = new char[operandCount - 1];
        char[] choices = {'+', '-', '*'};
        for (int i = 0; i < operandCount; i++) {
            operands[i] = 1 + random.nextInt(operandMax);
            if (i > 0) {
                operators[i - 1] = choices[random.nextInt(choices.length)];
            }
        }
        expression.append(operands[0]);
        for (int i = 0; i < operators.length; i++) {
            expression.append(' ').append(operators[i]).append(' ').append(operands[i + 1]);
        }
        long answer = evaluate(operands, operators);
        String challenge = expression.toString();
        String canonical = Long.toString(answer);
        String targetHash = derivation.targetHash(challenge, canonical);
        String wrappedKey = derivation.wrapKey(challenge, canonical, messageKey);

        return new GeneratedPuzzle(
                challenge,
                targetHash,
                0,
                wrappedKey,
                Map.of(
                        "puzzleType", PuzzleType.ARITHMETIC.name(),
                        "challenge", challenge,
                        "targetHash", targetHash,
                        "wrappedKey", wrappedKey,
                        "operands", operandCount
                )
        );
    }

    @Override
    public SolveResult solve(Puzzle puzzle, PuzzleSolveRequest request) {
        String raw = request == null ? null : request.getAnswer();
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("answer is required for the arithmetic puzzle");
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
        return "Evaluate the expression: " + puzzle.getChallenge() + " = ?";
    }

    static long evaluate(long[] operands, char[] operators) {
        // Two-pass evaluator: handle * first, then + / -. Operands are int-sized so overflow is not a concern.
        long[] firstPass = new long[operands.length];
        char[] firstOps = new char[operators.length];
        firstPass[0] = operands[0];
        int idx = 0;
        for (int i = 0; i < operators.length; i++) {
            if (operators[i] == '*') {
                firstPass[idx] = firstPass[idx] * operands[i + 1];
            } else {
                firstOps[idx] = operators[i];
                idx++;
                firstPass[idx] = operands[i + 1];
            }
        }
        long result = firstPass[0];
        for (int i = 0; i < idx; i++) {
            if (firstOps[i] == '+') {
                result += firstPass[i + 1];
            } else if (firstOps[i] == '-') {
                result -= firstPass[i + 1];
            }
        }
        return result;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
