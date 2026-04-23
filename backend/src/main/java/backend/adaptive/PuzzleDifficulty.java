package backend.adaptive;

public class PuzzleDifficulty {

    private final int maxIterations;
    private final int attemptsAllowed;
    private final int timeLimitSeconds;

    public PuzzleDifficulty(int maxIterations, int attemptsAllowed, int timeLimitSeconds) {
        this.maxIterations = maxIterations;
        this.attemptsAllowed = attemptsAllowed;
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public int getAttemptsAllowed() {
        return attemptsAllowed;
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }
}

