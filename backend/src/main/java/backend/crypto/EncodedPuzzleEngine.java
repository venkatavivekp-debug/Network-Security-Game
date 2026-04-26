package backend.crypto;

import backend.adaptive.PuzzleDifficulty;
import backend.dto.PuzzleSolveRequest;
import backend.exception.BadRequestException;
import backend.model.Puzzle;
import backend.model.PuzzleType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Encoded-message puzzle. The challenge is a Base64-encoded short phrase; the
 * receiver decodes it and submits the original cleartext (case-insensitive).
 * The canonical answer is the lowercased decoded phrase.
 */
@Component
public class EncodedPuzzleEngine implements PuzzleEngine {

    private static final List<String> PHRASES = List.of(
            "secure channel",
            "trust but verify",
            "encrypt all things",
            "zero trust network",
            "defense in depth",
            "session integrity",
            "quantum resistant",
            "audit and recover",
            "side channel guard",
            "rotate every key"
    );

    private final AnswerKeyDerivation derivation;
    private final SecureRandom random = new SecureRandom();

    public EncodedPuzzleEngine(AnswerKeyDerivation derivation) {
        this.derivation = derivation;
    }

    @Override
    public PuzzleType type() {
        return PuzzleType.ENCODED;
    }

    @Override
    public GeneratedPuzzle generate(byte[] messageKey, PuzzleDifficulty difficulty) {
        String phrase = PHRASES.get(random.nextInt(PHRASES.size()));
        String encoded = Base64.getEncoder().encodeToString(phrase.getBytes(StandardCharsets.UTF_8));
        String canonical = canonicalize(phrase);
        String targetHash = derivation.targetHash(encoded, canonical);
        String wrappedKey = derivation.wrapKey(encoded, canonical, messageKey);
        return new GeneratedPuzzle(
                encoded,
                targetHash,
                0,
                wrappedKey,
                Map.of(
                        "puzzleType", PuzzleType.ENCODED.name(),
                        "encoding", "BASE64",
                        "challenge", encoded,
                        "targetHash", targetHash,
                        "wrappedKey", wrappedKey
                )
        );
    }

    @Override
    public SolveResult solve(Puzzle puzzle, PuzzleSolveRequest request) {
        String raw = request == null ? null : request.getAnswer();
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("answer is required for the encoded-message puzzle");
        }
        String canonical = canonicalize(raw);
        if (!derivation.targetHash(puzzle.getChallenge(), canonical).equals(puzzle.getTargetHash())) {
            throw new BadRequestException("Incorrect answer");
        }
        byte[] recovered = derivation.unwrapKey(puzzle.getChallenge(), canonical, puzzle.getWrappedKey());
        return new SolveResult(recovered, null, derivation.answerHash(canonical));
    }

    @Override
    public String questionText(Puzzle puzzle) {
        return "Decode this Base64 string and submit the original phrase: " + puzzle.getChallenge();
    }

    private static String canonicalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
