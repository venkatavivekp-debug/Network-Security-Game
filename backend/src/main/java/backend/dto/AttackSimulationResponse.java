package backend.dto;

import backend.model.AlgorithmType;

public class AttackSimulationResponse {

    private Long messageId;
    private AlgorithmType algorithmType;
    private boolean classificationSuccess;
    private double classificationConfidence;
    private boolean selectiveJammingSuccess;
    private long timeRequiredMs;
    private String resultSummary;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    public boolean isClassificationSuccess() {
        return classificationSuccess;
    }

    public void setClassificationSuccess(boolean classificationSuccess) {
        this.classificationSuccess = classificationSuccess;
    }

    public double getClassificationConfidence() {
        return classificationConfidence;
    }

    public void setClassificationConfidence(double classificationConfidence) {
        this.classificationConfidence = classificationConfidence;
    }

    public boolean isSelectiveJammingSuccess() {
        return selectiveJammingSuccess;
    }

    public void setSelectiveJammingSuccess(boolean selectiveJammingSuccess) {
        this.selectiveJammingSuccess = selectiveJammingSuccess;
    }

    public long getTimeRequiredMs() {
        return timeRequiredMs;
    }

    public void setTimeRequiredMs(long timeRequiredMs) {
        this.timeRequiredMs = timeRequiredMs;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }
}
