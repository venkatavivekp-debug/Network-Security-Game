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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "puzzles")
public class Puzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "puzzle_type", nullable = false, length = 30)
    private PuzzleType puzzleType;

    @Column(name = "challenge", nullable = false, length = 256)
    private String challenge;

    @Column(name = "target_hash", nullable = false, length = 64)
    private String targetHash;

    @Column(name = "max_iterations", nullable = false)
    private int maxIterations;

    @Column(name = "wrapped_key", nullable = false, columnDefinition = "LONGTEXT")
    private String wrappedKey;

    @Column(name = "attempts_allowed", nullable = false)
    private int attemptsAllowed;

    @Column(name = "attempts_used", nullable = false)
    private int attemptsUsed;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "solved_at")
    private LocalDateTime solvedAt;

    @Column(name = "solved_nonce")
    private Integer solvedNonce;

    /**
     * For non-POW puzzle types, holds the AES message key (Base64) that was
     * unwrapped at solve time. For POW puzzles this stays null because the key
     * can be re-derived from {@link #solvedNonce}. This is the same security
     * profile as {@link #solvedNonce}: once a puzzle is solved the gating
     * material is no longer secret.
     */
    @Column(name = "recovered_key", columnDefinition = "LONGTEXT")
    private String recoveredKey;

    /**
     * Hex SHA-256 of the canonical answer that solved a non-POW puzzle. Stored
     * for audit / non-replay checks; never the raw answer itself.
     */
    @Column(name = "solved_answer_hash", length = 64)
    private String solvedAnswerHash;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public PuzzleType getPuzzleType() {
        return puzzleType;
    }

    public void setPuzzleType(PuzzleType puzzleType) {
        this.puzzleType = puzzleType;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getTargetHash() {
        return targetHash;
    }

    public void setTargetHash(String targetHash) {
        this.targetHash = targetHash;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public String getWrappedKey() {
        return wrappedKey;
    }

    public void setWrappedKey(String wrappedKey) {
        this.wrappedKey = wrappedKey;
    }

    public int getAttemptsAllowed() {
        return attemptsAllowed;
    }

    public void setAttemptsAllowed(int attemptsAllowed) {
        this.attemptsAllowed = attemptsAllowed;
    }

    public int getAttemptsUsed() {
        return attemptsUsed;
    }

    public void setAttemptsUsed(int attemptsUsed) {
        this.attemptsUsed = attemptsUsed;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getSolvedAt() {
        return solvedAt;
    }

    public void setSolvedAt(LocalDateTime solvedAt) {
        this.solvedAt = solvedAt;
    }

    public Integer getSolvedNonce() {
        return solvedNonce;
    }

    public void setSolvedNonce(Integer solvedNonce) {
        this.solvedNonce = solvedNonce;
    }

    public String getRecoveredKey() {
        return recoveredKey;
    }

    public void setRecoveredKey(String recoveredKey) {
        this.recoveredKey = recoveredKey;
    }

    public String getSolvedAnswerHash() {
        return solvedAnswerHash;
    }

    public void setSolvedAnswerHash(String solvedAnswerHash) {
        this.solvedAnswerHash = solvedAnswerHash;
    }
}

