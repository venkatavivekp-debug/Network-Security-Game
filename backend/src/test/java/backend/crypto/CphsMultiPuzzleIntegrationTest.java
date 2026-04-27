package backend.crypto;

import backend.adaptive.PuzzleDifficulty;
import backend.config.CphsProperties;
import backend.config.CryptoProperties;
import backend.config.PuzzleProperties;
import backend.crypto.PuzzleEngine.GeneratedPuzzle;
import backend.dto.PuzzleSolveRequest;
import backend.exception.BadRequestException;
import backend.model.Puzzle;
import backend.model.PuzzleType;
import backend.service.CPHSService;
import backend.service.PuzzleService;
import backend.util.EncryptionUtil;
import backend.util.HashUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that {@link CPHSService} can dispatch to every puzzle engine,
 * issue a typed challenge, and round-trip the plaintext when the receiver
 * supplies the correct answer. This is the closest thing to an end-to-end
 * test we can run without booting Spring.
 */
class CphsMultiPuzzleIntegrationTest {

    private static CPHSService cphs;
    private static PuzzleEngineRegistry registry;
    private static AnswerKeyDerivation derivation;
    private static EncryptionUtil enc;
    private static CryptoProperties cryptoProps;

    @BeforeAll
    static void setup() {
        PuzzleProperties puzzleProps = new PuzzleProperties();
        puzzleProps.setChallengeBytes(8);
        puzzleProps.setMaxIterations(5_000);
        puzzleProps.setKeyDerivationSalt("phase4-int-test-salt");

        CphsProperties cphsProps = new CphsProperties();
        cphsProps.setRandomKeyBytes(32);

        cryptoProps = new CryptoProperties();
        cryptoProps.setIvLengthBytes(12);
        cryptoProps.setAesTransformation("AES/GCM/NoPadding");
        cryptoProps.setGcmTagLengthBits(128);

        HashUtil hashUtil = new HashUtil();
        enc = new EncryptionUtil();
        enc.warmup();
        derivation = new AnswerKeyDerivation(hashUtil, puzzleProps);

        PuzzleService puzzleService = new PuzzleService(puzzleProps, hashUtil, enc);
        Sha256ProofOfWorkPuzzleEngine pow = new Sha256ProofOfWorkPuzzleEngine(puzzleService, puzzleProps, hashUtil);
        ArithmeticPuzzleEngine arith = new ArithmeticPuzzleEngine(derivation);
        EncodedPuzzleEngine encoded = new EncodedPuzzleEngine(derivation);
        PatternPuzzleEngine pattern = new PatternPuzzleEngine(derivation);

        registry = new PuzzleEngineRegistry(List.of(pow, arith, encoded, pattern));
        cphs = new CPHSService(cphsProps, cryptoProps, registry, puzzleService, enc, new ObjectMapper());
    }

    @Test
    void arithmeticPuzzleEndToEndUnlocksMessage() {
        EncryptionPackage pkg = cphs.encryptWithPuzzle("attack-vector-A1", PuzzleType.ARITHMETIC, null);
        assertNotNull(pkg.getMetadata());

        ArithmeticPuzzleEngine engine = (ArithmeticPuzzleEngine) registry.forType(PuzzleType.ARITHMETIC);
        // Recover challenge by re-running the engine with a known seed is not possible;
        // but the metadata exposes the challenge string, which we use to derive the answer.
        String metadata = pkg.getMetadata();
        String challenge = extractMetadataField(metadata, "challenge");
        long answer = ArithmeticPuzzleEngine.evaluate(parseOperands(challenge), parseOperators(challenge));

        Puzzle puzzle = puzzleFromMetadata(PuzzleType.ARITHMETIC, metadata);
        PuzzleSolveRequest req = answerRequest(Long.toString(answer));
        PuzzleEngine.SolveResult result = engine.solve(puzzle, req);

        String plain = enc.decrypt(pkg.getEncryptedContent(), result.recoveredKey(),
                cryptoProps.getAesTransformation(), cryptoProps.getGcmTagLengthBits());
        assertEquals("attack-vector-A1", plain);
    }

    @Test
    void encodedPuzzleEndToEndUnlocksMessage() {
        EncryptionPackage pkg = cphs.encryptWithPuzzle("staging-payload-B2", PuzzleType.ENCODED, null);
        String metadata = pkg.getMetadata();
        String challenge = extractMetadataField(metadata, "challenge");

        String decoded = new String(Base64.getDecoder().decode(challenge));
        Puzzle puzzle = puzzleFromMetadata(PuzzleType.ENCODED, metadata);
        PuzzleEngine.SolveResult result = registry.forType(PuzzleType.ENCODED)
                .solve(puzzle, answerRequest(decoded));

        String plain = enc.decrypt(pkg.getEncryptedContent(), result.recoveredKey(),
                cryptoProps.getAesTransformation(), cryptoProps.getGcmTagLengthBits());
        assertEquals("staging-payload-B2", plain);
    }

    @Test
    void patternPuzzleEndToEndUnlocksMessage() {
        EncryptionPackage pkg = cphs.encryptWithPuzzle("rotation-keys-C3", PuzzleType.PATTERN, null);
        String metadata = pkg.getMetadata();
        String challenge = extractMetadataField(metadata, "challenge");

        long expectedNext = inferNext(challenge);
        Puzzle puzzle = puzzleFromMetadata(PuzzleType.PATTERN, metadata);
        PuzzleEngine.SolveResult result = registry.forType(PuzzleType.PATTERN)
                .solve(puzzle, answerRequest(Long.toString(expectedNext)));

        String plain = enc.decrypt(pkg.getEncryptedContent(), result.recoveredKey(),
                cryptoProps.getAesTransformation(), cryptoProps.getGcmTagLengthBits());
        assertEquals("rotation-keys-C3", plain);
    }

    @Test
    void wrongAnswerNeverYieldsValidKey() {
        EncryptionPackage pkg = cphs.encryptWithPuzzle("never-leak-D4", PuzzleType.ARITHMETIC, null);
        Puzzle puzzle = puzzleFromMetadata(PuzzleType.ARITHMETIC, pkg.getMetadata());
        // 99999 is virtually never the correct answer for our small expressions.
        assertThrows(BadRequestException.class,
                () -> registry.forType(PuzzleType.ARITHMETIC).solve(puzzle, answerRequest("99999")));
    }

    @Test
    void puzzleTypesProduceDistinctChallenges() {
        // Same plaintext twice with different puzzle types should produce
        // structurally different challenges in metadata. This guards against
        // accidental fallthrough to POW for non-POW types.
        String arithMeta = cphs.encryptWithPuzzle("x", PuzzleType.ARITHMETIC, null).getMetadata();
        String encMeta = cphs.encryptWithPuzzle("x", PuzzleType.ENCODED, null).getMetadata();
        assertNotEquals(extractMetadataField(arithMeta, "puzzleType"), extractMetadataField(encMeta, "puzzleType"));
    }

    @Test
    void powDifficultyIsHonored() {
        PuzzleDifficulty difficulty = new PuzzleDifficulty(1500, 3, 60);
        EncryptionPackage pkg = cphs.encryptWithPuzzle("pow-payload", PuzzleType.POW_SHA256, difficulty);
        String maxItersStr = extractMetadataField(pkg.getMetadata(), "maxIterations");
        assertEquals(1500, Integer.parseInt(maxItersStr));
    }

    private static String extractMetadataField(String metadataJson, String key) {
        try {
            return new ObjectMapper().readTree(metadataJson).get(key).asText();
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse metadata", e);
        }
    }

    private static Puzzle puzzleFromMetadata(PuzzleType type, String metadataJson) {
        try {
            var node = new ObjectMapper().readTree(metadataJson);
            Puzzle puzzle = new Puzzle();
            puzzle.setPuzzleType(type);
            puzzle.setChallenge(node.get("challenge").asText());
            puzzle.setTargetHash(node.has("targetHash") ? node.get("targetHash").asText() : null);
            puzzle.setMaxIterations(node.has("maxIterations") ? node.get("maxIterations").asInt() : 0);
            puzzle.setWrappedKey(node.get("wrappedKey").asText());
            puzzle.setAttemptsAllowed(3);
            puzzle.setAttemptsUsed(0);
            return puzzle;
        } catch (Exception e) {
            throw new IllegalStateException("Could not build puzzle from metadata", e);
        }
    }

    private static PuzzleSolveRequest answerRequest(String answer) {
        PuzzleSolveRequest request = new PuzzleSolveRequest();
        request.setAnswer(answer);
        return request;
    }

    private static long[] parseOperands(String expr) {
        String[] parts = expr.split(" ");
        long[] operands = new long[(parts.length + 1) / 2];
        int idx = 0;
        for (int i = 0; i < parts.length; i += 2) operands[idx++] = Long.parseLong(parts[i]);
        return Arrays.copyOf(operands, idx);
    }

    private static char[] parseOperators(String expr) {
        String[] parts = expr.split(" ");
        char[] operators = new char[parts.length / 2];
        int idx = 0;
        for (int i = 1; i < parts.length; i += 2) operators[idx++] = parts[i].charAt(0);
        return Arrays.copyOf(operators, idx);
    }

    private static long inferNext(String challenge) {
        String stripped = challenge.replace(", ?", "");
        String[] parts = stripped.split(", ");
        long[] seq = Arrays.stream(parts).mapToLong(Long::parseLong).toArray();
        long d1 = seq[1] - seq[0];
        long d2 = seq[2] - seq[1];
        long d3 = seq[3] - seq[2];
        // Arithmetic progression: require all three diffs match to avoid
        // false positives for some Fibonacci-like starts (e.g. 1,2,3,5,...).
        if (d1 == d2 && d2 == d3) return seq[3] + d1;

        // Geometric progression: use exact integer ratio checks.
        if (seq[0] != 0 && seq[1] != 0 && seq[1] % seq[0] == 0 && seq[2] % seq[1] == 0) {
            long r1 = seq[1] / seq[0];
            long r2 = seq[2] / seq[1];
            if (r1 == r2) return seq[3] * r1;
        }

        // Fibonacci-like: next = last + previous
        return seq[3] + seq[2];
    }
}
