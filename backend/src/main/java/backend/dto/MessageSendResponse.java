package backend.dto;

import backend.model.AlgorithmType;

import java.time.LocalDateTime;

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
    private String escalationReason;
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
