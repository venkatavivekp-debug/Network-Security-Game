package backend.service;

import backend.config.PuzzleProperties;
import backend.util.EncryptionUtil;
import backend.util.HashUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuzzleServiceTest {

    @Test
    void shouldGeneratePuzzleAndRecoverOriginalKey() {
        PuzzleProperties properties = new PuzzleProperties();
        properties.setChallengeBytes(8);
        properties.setMaxIterations(5000);
        properties.setSimulatedDelayMs(0);
        properties.setKeyDerivationSalt("phase4-test-salt");

        HashUtil hashUtil = new HashUtil();
        EncryptionUtil encryptionUtil = new EncryptionUtil();
        encryptionUtil.warmup();

        PuzzleService puzzleService = new PuzzleService(properties, hashUtil, encryptionUtil);
        byte[] originalKey = encryptionUtil.randomBytes(32);

        PuzzleService.PuzzlePackagingResult packageResult = puzzleService.createPuzzlePackage(originalKey);
        PuzzleService.PuzzleSolveResult solveResult = puzzleService.solveAndRecoverKey(
                packageResult.getDescriptor().getChallenge(),
                packageResult.getDescriptor().getTargetHash(),
                packageResult.getDescriptor().getMaxIterations(),
                packageResult.getWrappedKeyBase64()
        );

        assertArrayEquals(originalKey, solveResult.getRecoveredKey());
        assertTrue(solveResult.getSolveTimeMs() >= 0);
    }
}
