package backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Persisted, per-user behavioral profile that the adaptive engine reads from
 * and writes to. The profile is intentionally small and bounded: it does not
 * store plaintext, does not store the original puzzle nonce, and does not
 * store IP/user-agent values directly (only a fingerprint hash for the
 * "trusted device" check, which already lives on {@link User}).
 *
 * <p>The fields here are the ones the report and paper care about: how often
 * the user solves puzzles, how often they fail, how long they take, and how
 * many recent failures are still "fresh" before they decay back to zero.
 */
@Entity
@Table(name = "user_behavior_profiles")
public class UserBehaviorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "puzzle_attempts", nullable = false)
    private long puzzleAttempts;

    @Column(name = "puzzle_successes", nullable = false)
    private long puzzleSuccesses;

    @Column(name = "puzzle_failures", nullable = false)
    private long puzzleFailures;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    /** Mean solve time across all successful CPHS solves, in milliseconds. */
    @Column(name = "avg_solve_time_ms", nullable = false)
    private double avgSolveTimeMs;

    /** Number of admin-supervised recoveries the user has been through. */
    @Column(name = "recovery_events", nullable = false)
    private int recoveryEvents;

    /** Last time any field on this profile was updated. */
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    /** Last time the user successfully solved a CPHS puzzle. */
    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    /** Last time the user failed a CPHS puzzle attempt. */
    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    public UserBehaviorProfile() {
    }

    public UserBehaviorProfile(Long userId) {
        this.userId = userId;
        this.lastUpdated = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public long getPuzzleAttempts() {
        return puzzleAttempts;
    }

    public void setPuzzleAttempts(long puzzleAttempts) {
        this.puzzleAttempts = puzzleAttempts;
    }

    public long getPuzzleSuccesses() {
        return puzzleSuccesses;
    }

    public void setPuzzleSuccesses(long puzzleSuccesses) {
        this.puzzleSuccesses = puzzleSuccesses;
    }

    public long getPuzzleFailures() {
        return puzzleFailures;
    }

    public void setPuzzleFailures(long puzzleFailures) {
        this.puzzleFailures = puzzleFailures;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(int consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public double getAvgSolveTimeMs() {
        return avgSolveTimeMs;
    }

    public void setAvgSolveTimeMs(double avgSolveTimeMs) {
        this.avgSolveTimeMs = avgSolveTimeMs;
    }

    public int getRecoveryEvents() {
        return recoveryEvents;
    }

    public void setRecoveryEvents(int recoveryEvents) {
        this.recoveryEvents = recoveryEvents;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(LocalDateTime lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public LocalDateTime getLastFailureAt() {
        return lastFailureAt;
    }

    public void setLastFailureAt(LocalDateTime lastFailureAt) {
        this.lastFailureAt = lastFailureAt;
    }
}
