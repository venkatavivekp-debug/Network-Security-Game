package backend.crypto;

public class PuzzleDescriptor {

    private final String challenge;
    private final String targetHash;
    private final int maxIterations;

    public PuzzleDescriptor(String challenge, String targetHash, int maxIterations) {
        this.challenge = challenge;
        this.targetHash = targetHash;
        this.maxIterations = maxIterations;
    }

    public String getChallenge() {
        return challenge;
    }

    public String getTargetHash() {
        return targetHash;
    }

    public int getMaxIterations() {
        return maxIterations;
    }
}
