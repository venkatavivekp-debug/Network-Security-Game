package backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.puzzle")
public class PuzzleProperties {

    private int maxIterations;
    private int challengeBytes;
    private int simulatedDelayMs;
    private String keyDerivationSalt;

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
}
