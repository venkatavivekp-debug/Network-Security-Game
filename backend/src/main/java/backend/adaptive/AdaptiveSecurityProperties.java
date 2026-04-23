package backend.adaptive;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.adaptive")
public class AdaptiveSecurityProperties {

    private double elevatedThreshold = 0.45;
    private double highThreshold = 0.70;
    private double criticalThreshold = 0.88;

    private double weightFailedAttempts = 0.25;
    private double weightUnusualLoginTime = 0.18;
    private double weightNewDevice = 0.22;
    private double weightAttackIntensity = 0.20;
    private double weightBehaviorDeviation = 0.15;

    private int lockMinutesOnCritical = 15;

    public double getElevatedThreshold() {
        return elevatedThreshold;
    }

    public void setElevatedThreshold(double elevatedThreshold) {
        this.elevatedThreshold = elevatedThreshold;
    }

    public double getHighThreshold() {
        return highThreshold;
    }

    public void setHighThreshold(double highThreshold) {
        this.highThreshold = highThreshold;
    }

    public double getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(double criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public double getWeightFailedAttempts() {
        return weightFailedAttempts;
    }

    public void setWeightFailedAttempts(double weightFailedAttempts) {
        this.weightFailedAttempts = weightFailedAttempts;
    }

    public double getWeightUnusualLoginTime() {
        return weightUnusualLoginTime;
    }

    public void setWeightUnusualLoginTime(double weightUnusualLoginTime) {
        this.weightUnusualLoginTime = weightUnusualLoginTime;
    }

    public double getWeightNewDevice() {
        return weightNewDevice;
    }

    public void setWeightNewDevice(double weightNewDevice) {
        this.weightNewDevice = weightNewDevice;
    }

    public double getWeightAttackIntensity() {
        return weightAttackIntensity;
    }

    public void setWeightAttackIntensity(double weightAttackIntensity) {
        this.weightAttackIntensity = weightAttackIntensity;
    }

    public double getWeightBehaviorDeviation() {
        return weightBehaviorDeviation;
    }

    public void setWeightBehaviorDeviation(double weightBehaviorDeviation) {
        this.weightBehaviorDeviation = weightBehaviorDeviation;
    }

    public int getLockMinutesOnCritical() {
        return lockMinutesOnCritical;
    }

    public void setLockMinutesOnCritical(int lockMinutesOnCritical) {
        this.lockMinutesOnCritical = lockMinutesOnCritical;
    }
}

