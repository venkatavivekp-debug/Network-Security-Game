package backend.simulation;

public class AttackSimulationResult {

    private final boolean classificationSuccess;
    private final double classificationConfidence;
    private final boolean selectiveJammingSuccess;
    private final long timeRequiredMs;
    private final String summary;

    public AttackSimulationResult(
            boolean classificationSuccess,
            double classificationConfidence,
            boolean selectiveJammingSuccess,
            long timeRequiredMs,
            String summary
    ) {
        this.classificationSuccess = classificationSuccess;
        this.classificationConfidence = classificationConfidence;
        this.selectiveJammingSuccess = selectiveJammingSuccess;
        this.timeRequiredMs = timeRequiredMs;
        this.summary = summary;
    }

    public boolean isClassificationSuccess() {
        return classificationSuccess;
    }

    public double getClassificationConfidence() {
        return classificationConfidence;
    }

    public boolean isSelectiveJammingSuccess() {
        return selectiveJammingSuccess;
    }

    public long getTimeRequiredMs() {
        return timeRequiredMs;
    }

    public String getSummary() {
        return summary;
    }
}
