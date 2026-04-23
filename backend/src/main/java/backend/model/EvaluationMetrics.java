package backend.model;

public class EvaluationMetrics {

    private double attackSuccessRate;
    private double compromiseRatio;
    private double averageRecoveryTime;
    private double resilienceScore;
    private double userEffortScore;
    private double falsePositiveRate;

    public double getAttackSuccessRate() {
        return attackSuccessRate;
    }

    public void setAttackSuccessRate(double attackSuccessRate) {
        this.attackSuccessRate = attackSuccessRate;
    }

    public double getCompromiseRatio() {
        return compromiseRatio;
    }

    public void setCompromiseRatio(double compromiseRatio) {
        this.compromiseRatio = compromiseRatio;
    }

    public double getAverageRecoveryTime() {
        return averageRecoveryTime;
    }

    public void setAverageRecoveryTime(double averageRecoveryTime) {
        this.averageRecoveryTime = averageRecoveryTime;
    }

    public double getResilienceScore() {
        return resilienceScore;
    }

    public void setResilienceScore(double resilienceScore) {
        this.resilienceScore = resilienceScore;
    }

    public double getUserEffortScore() {
        return userEffortScore;
    }

    public void setUserEffortScore(double userEffortScore) {
        this.userEffortScore = userEffortScore;
    }

    public double getFalsePositiveRate() {
        return falsePositiveRate;
    }

    public void setFalsePositiveRate(double falsePositiveRate) {
        this.falsePositiveRate = falsePositiveRate;
    }
}

