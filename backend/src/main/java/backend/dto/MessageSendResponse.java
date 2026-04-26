package backend.dto;

import backend.model.AlgorithmType;

import java.time.LocalDateTime;
import java.util.List;

public class MessageSendResponse {

    private Long messageId;
    private String senderUsername;
    private String receiverUsername;
    private AlgorithmType requestedAlgorithmType;
    private AlgorithmType effectiveAlgorithmType;
    private boolean escalated;
    private boolean communicationHold;
    private Double riskScore;
    private String riskLevel;
    private List<String> riskReasons;
    private String escalationReason;
    private String recoveryState;
    private boolean adminReviewRequired;
    private String recoverySummary;
    private List<String> recoveryNextSteps;
    private String connectionSecurityState;
    private List<String> connectionShiftedSignals;
    private String warningMessage;
    private LocalDateTime createdAt;
    private String status;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    /** Backward-compatible accessor (effective mode). */
    public AlgorithmType getAlgorithmType() {
        return effectiveAlgorithmType;
    }

    /** Backward-compatible mutator (effective mode). */
    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.effectiveAlgorithmType = algorithmType;
    }

    public AlgorithmType getRequestedAlgorithmType() {
        return requestedAlgorithmType;
    }

    public void setRequestedAlgorithmType(AlgorithmType requestedAlgorithmType) {
        this.requestedAlgorithmType = requestedAlgorithmType;
    }

    public AlgorithmType getEffectiveAlgorithmType() {
        return effectiveAlgorithmType;
    }

    public void setEffectiveAlgorithmType(AlgorithmType effectiveAlgorithmType) {
        this.effectiveAlgorithmType = effectiveAlgorithmType;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    public boolean isCommunicationHold() {
        return communicationHold;
    }

    public void setCommunicationHold(boolean communicationHold) {
        this.communicationHold = communicationHold;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    public List<String> getRiskReasons() {
        return riskReasons;
    }

    public void setRiskReasons(List<String> riskReasons) {
        this.riskReasons = riskReasons;
    }

    public String getRecoveryState() {
        return recoveryState;
    }

    public void setRecoveryState(String recoveryState) {
        this.recoveryState = recoveryState;
    }

    public boolean isAdminReviewRequired() {
        return adminReviewRequired;
    }

    public void setAdminReviewRequired(boolean adminReviewRequired) {
        this.adminReviewRequired = adminReviewRequired;
    }

    public String getRecoverySummary() {
        return recoverySummary;
    }

    public void setRecoverySummary(String recoverySummary) {
        this.recoverySummary = recoverySummary;
    }

    public List<String> getRecoveryNextSteps() {
        return recoveryNextSteps;
    }

    public void setRecoveryNextSteps(List<String> recoveryNextSteps) {
        this.recoveryNextSteps = recoveryNextSteps;
    }

    public String getConnectionSecurityState() {
        return connectionSecurityState;
    }

    public void setConnectionSecurityState(String connectionSecurityState) {
        this.connectionSecurityState = connectionSecurityState;
    }

    public List<String> getConnectionShiftedSignals() {
        return connectionShiftedSignals;
    }

    public void setConnectionShiftedSignals(List<String> connectionShiftedSignals) {
        this.connectionShiftedSignals = connectionShiftedSignals;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
