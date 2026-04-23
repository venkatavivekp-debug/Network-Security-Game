package backend.service;

import backend.config.PuzzleProperties;
import backend.crypto.PuzzleDescriptor;
import backend.exception.BadRequestException;
import backend.util.EncryptionUtil;
import backend.util.HashUtil;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PuzzleService {

    private final PuzzleProperties puzzleProperties;
    private final HashUtil hashUtil;
    private final EncryptionUtil encryptionUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    public PuzzleService(PuzzleProperties puzzleProperties, HashUtil hashUtil, EncryptionUtil encryptionUtil) {
        this.puzzleProperties = puzzleProperties;
        this.hashUtil = hashUtil;
        this.encryptionUtil = encryptionUtil;
    }

    public PuzzlePackagingResult createPuzzlePackage(byte[] messageKey) {
        String challenge = Base64.getEncoder().encodeToString(encryptionUtil.randomBytes(puzzleProperties.getChallengeBytes()));
        int nonce = secureRandom.nextInt(Math.max(1, puzzleProperties.getMaxIterations()));
        String targetHash = hashUtil.sha256Hex(challenge + ":" + nonce);

        byte[] derivedKey = derivePuzzleKey(challenge, nonce);
        byte[] wrappedKey = xor(messageKey, derivedKey);

        PuzzleDescriptor descriptor = new PuzzleDescriptor(challenge, targetHash, puzzleProperties.getMaxIterations());
        String wrappedKeyBase64 = Base64.getEncoder().encodeToString(wrappedKey);

        return new PuzzlePackagingResult(descriptor, wrappedKeyBase64);
    }

    public PuzzleSolveResult solveAndRecoverKey(String challenge, String targetHash, int maxIterations, String wrappedKeyBase64) {
        long start = System.currentTimeMillis();

        Integer solvedNonce = null;
        for (int nonce = 0; nonce <= maxIterations; nonce++) {
            String candidate = hashUtil.sha256Hex(challenge + ":" + nonce);
            if (candidate.equals(targetHash)) {
                solvedNonce = nonce;
                break;
            }
        }

        if (solvedNonce == null) {
            throw new BadRequestException("Puzzle could not be solved within configured iterations");
        }

        byte[] derivedKey = derivePuzzleKey(challenge, solvedNonce);
        byte[] wrappedKey = Base64.getDecoder().decode(wrappedKeyBase64);
        byte[] recoveredKey = xor(wrappedKey, derivedKey);

        simulateDelay();
        long totalTime = System.currentTimeMillis() - start;

        return new PuzzleSolveResult(recoveredKey, totalTime, solvedNonce);
    }

    private byte[] derivePuzzleKey(String challenge, int nonce) {
        String material = challenge + ":" + nonce + ":" + puzzleProperties.getKeyDerivationSalt();
        return hashUtil.sha256Bytes(material);
    }

    private byte[] xor(byte[] left, byte[] right) {
        byte[] result = new byte[left.length];
        for (int i = 0; i < left.length; i++) {
            result[i] = (byte) (left[i] ^ right[i % right.length]);
        }
        return result;
    }

    private void simulateDelay() {
        try {
            Thread.sleep(Math.max(0, puzzleProperties.getSimulatedDelayMs()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Puzzle delay interrupted", ex);
        }
    }

    public static class PuzzlePackagingResult {
        private final PuzzleDescriptor descriptor;
        private final String wrappedKeyBase64;

        public PuzzlePackagingResult(PuzzleDescriptor descriptor, String wrappedKeyBase64) {
            this.descriptor = descriptor;
            this.wrappedKeyBase64 = wrappedKeyBase64;
        }

        public PuzzleDescriptor getDescriptor() {
            return descriptor;
        }

        public String getWrappedKeyBase64() {
            return wrappedKeyBase64;
        }
    }

    public static class PuzzleSolveResult {
        private final byte[] recoveredKey;
        private final long solveTimeMs;
        private final int solvedNonce;

        public PuzzleSolveResult(byte[] recoveredKey, long solveTimeMs, int solvedNonce) {
            this.recoveredKey = recoveredKey;
            this.solveTimeMs = solveTimeMs;
            this.solvedNonce = solvedNonce;
        }

        public byte[] getRecoveredKey() {
            return recoveredKey;
        }

        public long getSolveTimeMs() {
            return solveTimeMs;
        }

        public int getSolvedNonce() {
            return solvedNonce;
        }
    }
}
