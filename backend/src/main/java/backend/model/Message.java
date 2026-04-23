package backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.OneToOne;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "encrypted_content", nullable = false, columnDefinition = "LONGTEXT")
    private String encryptedContent;

    @Column(name = "original_hash", length = 64)
    private String originalHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm_type", nullable = false, length = 20)
    private AlgorithmType algorithmType;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_algorithm_type", length = 20)
    private AlgorithmType requestedAlgorithmType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status = MessageStatus.LOCKED;

    @Column(name = "risk_score_at_send")
    private Double riskScoreAtSend;

    @Column(name = "risk_level_at_send", length = 20)
    private String riskLevelAtSend;

    @Column(name = "hold_reason", length = 200)
    private String holdReason;

    @Column(name = "metadata", columnDefinition = "LONGTEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "message", fetch = FetchType.LAZY)
    private Puzzle puzzle;

    public Message() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getEncryptedContent() {
        return encryptedContent;
    }

    public void setEncryptedContent(String encryptedContent) {
        this.encryptedContent = encryptedContent;
    }

    public String getOriginalHash() {
        return originalHash;
    }

    public void setOriginalHash(String originalHash) {
        this.originalHash = originalHash;
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

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public Double getRiskScoreAtSend() {
        return riskScoreAtSend;
    }

    public void setRiskScoreAtSend(Double riskScoreAtSend) {
        this.riskScoreAtSend = riskScoreAtSend;
    }

    public String getRiskLevelAtSend() {
        return riskLevelAtSend;
    }

    public void setRiskLevelAtSend(String riskLevelAtSend) {
        this.riskLevelAtSend = riskLevelAtSend;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public void setHoldReason(String holdReason) {
        this.holdReason = holdReason;
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

    public Puzzle getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(Puzzle puzzle) {
        this.puzzle = puzzle;
    }
}
