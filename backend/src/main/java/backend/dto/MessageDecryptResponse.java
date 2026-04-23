package backend.dto;

import backend.model.AlgorithmType;

public class MessageDecryptResponse {

    private Long messageId;
    private AlgorithmType algorithmType;
    private String decryptedContent;
    private long puzzleSolveTimeMs;
    private String status;

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

    public String getDecryptedContent() {
        return decryptedContent;
    }

    public void setDecryptedContent(String decryptedContent) {
        this.decryptedContent = decryptedContent;
    }

    public long getPuzzleSolveTimeMs() {
        return puzzleSolveTimeMs;
    }

    public void setPuzzleSolveTimeMs(long puzzleSolveTimeMs) {
        this.puzzleSolveTimeMs = puzzleSolveTimeMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
