package backend.crypto;

import backend.adaptive.PuzzleDifficulty;
import backend.config.PuzzleProperties;
import backend.dto.PuzzleSolveRequest;
import backend.exception.BadRequestException;
import backend.model.Puzzle;
import backend.model.PuzzleType;
import backend.service.PuzzleService;
import backend.util.HashUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Existing SHA-256 proof-of-work puzzle. The receiver searches for a nonce
 * {@code n} such that {@code SHA-256(challenge + ":" + n) == targetHash}. This
 * engine reuses {@link PuzzleService} so the cryptographic core stays
 * unchanged from the original implementation.
 */
@Component
public class Sha256ProofOfWorkPuzzleEngine implements PuzzleEngine {

    private final PuzzleService puzzleService;
    private final PuzzleProperties puzzleProperties;
    private final HashUtil hashUtil;

    public Sha256ProofOfWorkPuzzleEngine(
            PuzzleService puzzleService,
            PuzzleProperties puzzleProperties,
            HashUtil hashUtil
    ) {
        this.puzzleService = puzzleService;
        this.puzzleProperties = puzzleProperties;
        this.hashUtil = hashUtil;
    }

    @Override
    public PuzzleType type() {
        return PuzzleType.POW_SHA256;
    }

    @Override
    public GeneratedPuzzle generate(byte[] messageKey, PuzzleDifficulty difficulty) {
        int maxIterations = difficulty == null
                ? puzzleProperties.getMaxIterations()
                : difficulty.getMaxIterations();
        PuzzleService.PuzzlePackagingResult result = puzzleService.createPuzzlePackage(messageKey, maxIterations);
        PuzzleDescriptor descriptor = result.getDescriptor();
        return new GeneratedPuzzle(
                descriptor.getChallenge(),
                descriptor.getTargetHash(),
                descriptor.getMaxIterations(),
                result.getWrappedKeyBase64(),
                Map.of(
                        "puzzleType", PuzzleType.POW_SHA256.name(),
                        "challenge", descriptor.getChallenge(),
                        "targetHash", descriptor.getTargetHash(),
                        "maxIterations", descriptor.getMaxIterations(),
                        "wrappedKey", result.getWrappedKeyBase64()
                )
        );
    }

    @Override
    public SolveResult solve(Puzzle puzzle, PuzzleSolveRequest request) {
        if (request == null || request.getNonce() == null) {
            throw new BadRequestException("nonce is required for SHA-256 proof-of-work puzzles");
        }
        int nonce = request.getNonce();
        if (nonce < 0 || nonce >= puzzle.getMaxIterations()) {
            throw new BadRequestException("nonce must be in [0, maxIterations)");
        }
        byte[] recovered = puzzleService.recoverKeyFromNonce(
                puzzle.getChallenge(),
                puzzle.getTargetHash(),
                nonce,
                puzzle.getWrappedKey()
        );
        String answerHash = hashUtil.sha256Hex(Integer.toString(nonce));
        return new SolveResult(recovered, nonce, answerHash);
    }

    @Override
    public String questionText(Puzzle puzzle) {
        return "Find a nonce n in [0, " + puzzle.getMaxIterations() + ") such that "
                + "SHA-256(challenge + ':' + n) equals targetHash. Difficulty scales with risk.";
    }
}
