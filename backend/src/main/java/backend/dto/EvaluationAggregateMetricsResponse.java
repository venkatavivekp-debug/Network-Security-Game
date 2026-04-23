package backend.dto;

public class EvaluationAggregateMetricsResponse {

    private int runsExecuted;

    private double averageFinalCompromisedNodes;
    private double averageCompromiseRatio;
    private double averageResilienceScore;
    private double averageAttackEfficiency;
    private double averageDefenseEfficiency;
    private double averageDeceptionEffectiveness;
    private double averageMtdEffectiveness;
    private double averageMeanTimeToCompromise;
    private double averageAttackPathDepth;

    private double stdDevFinalCompromisedNodes;
    private double stdDevCompromiseRatio;
    private double stdDevResilienceScore;
    private double stdDevAttackEfficiency;
    private double stdDevDefenseEfficiency;
    private double stdDevMeanTimeToCompromise;

    public int getRunsExecuted() {
        return runsExecuted;
    }

    public void setRunsExecuted(int runsExecuted) {
        this.runsExecuted = runsExecuted;
    }

    public double getAverageFinalCompromisedNodes() {
        return averageFinalCompromisedNodes;
    }

    public void setAverageFinalCompromisedNodes(double averageFinalCompromisedNodes) {
        this.averageFinalCompromisedNodes = averageFinalCompromisedNodes;
    }

    public double getAverageCompromiseRatio() {
        return averageCompromiseRatio;
    }

    public void setAverageCompromiseRatio(double averageCompromiseRatio) {
        this.averageCompromiseRatio = averageCompromiseRatio;
    }

    public double getAverageResilienceScore() {
        return averageResilienceScore;
    }

    public void setAverageResilienceScore(double averageResilienceScore) {
        this.averageResilienceScore = averageResilienceScore;
    }

    public double getAverageAttackEfficiency() {
        return averageAttackEfficiency;
    }

    public void setAverageAttackEfficiency(double averageAttackEfficiency) {
        this.averageAttackEfficiency = averageAttackEfficiency;
    }

    public double getAverageDefenseEfficiency() {
        return averageDefenseEfficiency;
    }

    public void setAverageDefenseEfficiency(double averageDefenseEfficiency) {
        this.averageDefenseEfficiency = averageDefenseEfficiency;
    }

    public double getAverageDeceptionEffectiveness() {
        return averageDeceptionEffectiveness;
    }

    public void setAverageDeceptionEffectiveness(double averageDeceptionEffectiveness) {
        this.averageDeceptionEffectiveness = averageDeceptionEffectiveness;
    }

    public double getAverageMtdEffectiveness() {
        return averageMtdEffectiveness;
    }

    public void setAverageMtdEffectiveness(double averageMtdEffectiveness) {
        this.averageMtdEffectiveness = averageMtdEffectiveness;
    }

    public double getAverageMeanTimeToCompromise() {
        return averageMeanTimeToCompromise;
    }

    public void setAverageMeanTimeToCompromise(double averageMeanTimeToCompromise) {
        this.averageMeanTimeToCompromise = averageMeanTimeToCompromise;
    }

    public double getAverageAttackPathDepth() {
        return averageAttackPathDepth;
    }

    public void setAverageAttackPathDepth(double averageAttackPathDepth) {
        this.averageAttackPathDepth = averageAttackPathDepth;
    }

    public double getStdDevFinalCompromisedNodes() {
        return stdDevFinalCompromisedNodes;
    }

    public void setStdDevFinalCompromisedNodes(double stdDevFinalCompromisedNodes) {
        this.stdDevFinalCompromisedNodes = stdDevFinalCompromisedNodes;
    }

    public double getStdDevCompromiseRatio() {
        return stdDevCompromiseRatio;
    }

    public void setStdDevCompromiseRatio(double stdDevCompromiseRatio) {
        this.stdDevCompromiseRatio = stdDevCompromiseRatio;
    }

    public double getStdDevResilienceScore() {
        return stdDevResilienceScore;
    }

    public void setStdDevResilienceScore(double stdDevResilienceScore) {
        this.stdDevResilienceScore = stdDevResilienceScore;
    }

    public double getStdDevAttackEfficiency() {
        return stdDevAttackEfficiency;
    }

    public void setStdDevAttackEfficiency(double stdDevAttackEfficiency) {
        this.stdDevAttackEfficiency = stdDevAttackEfficiency;
    }

    public double getStdDevDefenseEfficiency() {
        return stdDevDefenseEfficiency;
    }

    public void setStdDevDefenseEfficiency(double stdDevDefenseEfficiency) {
        this.stdDevDefenseEfficiency = stdDevDefenseEfficiency;
    }

    public double getStdDevMeanTimeToCompromise() {
        return stdDevMeanTimeToCompromise;
    }

    public void setStdDevMeanTimeToCompromise(double stdDevMeanTimeToCompromise) {
        this.stdDevMeanTimeToCompromise = stdDevMeanTimeToCompromise;
    }
}
