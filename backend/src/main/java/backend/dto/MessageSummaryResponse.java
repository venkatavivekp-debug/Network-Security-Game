package backend.dto;

import backend.model.AlgorithmType;

import java.time.LocalDateTime;

public class MessageSummaryResponse {

    private Long id;
    private String senderUsername;
    private String receiverUsername;
    private String encryptedContent;
    private AlgorithmType algorithmType;
    private AlgorithmType requestedAlgorithmType;
    private String status;
    private Double riskScore;
    private String riskLevel;
    private String warning;
    private String warningMessage;
    private String recoveryState;
    private boolean adminReviewRequired;
    private String metadata;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEncryptedContent() {
        return encryptedContent;
    }

    public void setEncryptedContent(String encryptedContent) {
        this.encryptedContent = encryptedContent;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    public AlgorithmType getRequestedAlgorithmType() {
        return requestedAlgorithmType;
    }

    public void setRequestedAlgorithmType(AlgorithmType requestedAlgorithmType) {
        this.requestedAlgorithmType = requestedAlgorithmType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
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

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
