package backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.puzzle")
public class PuzzleProperties {

    private int maxIterations;
    private int challengeBytes;
    private int simulatedDelayMs;
    private String keyDerivationSalt;
    private int attemptsAllowed = 3;
    private int timeLimitSeconds = 300;
    private int minAdaptiveIterations = 15000;

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getChallengeBytes() {
        return challengeBytes;
    }

    public void setChallengeBytes(int challengeBytes) {
        this.challengeBytes = challengeBytes;
    }

    public int getSimulatedDelayMs() {
        return simulatedDelayMs;
    }

    public void setSimulatedDelayMs(int simulatedDelayMs) {
        this.simulatedDelayMs = simulatedDelayMs;
    }

    public String getKeyDerivationSalt() {
        return keyDerivationSalt;
    }

    public void setKeyDerivationSalt(String keyDerivationSalt) {
        this.keyDerivationSalt = keyDerivationSalt;
    }

    public int getAttemptsAllowed() {
        return attemptsAllowed;
    }

    public void setAttemptsAllowed(int attemptsAllowed) {
        this.attemptsAllowed = attemptsAllowed;
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(int timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public int getMinAdaptiveIterations() {
        return minAdaptiveIterations;
    }

    public void setMinAdaptiveIterations(int minAdaptiveIterations) {
        this.minAdaptiveIterations = minAdaptiveIterations;
    }
}
