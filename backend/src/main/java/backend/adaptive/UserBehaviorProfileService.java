package backend.adaptive;

import backend.model.User;
import backend.model.UserBehaviorProfile;
import backend.repository.UserBehaviorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Reads and updates the persistent user behavior profile.
 *
 * <p>The contract this service guarantees:
 * <ul>
 *   <li>Failure counters increase on every failed puzzle attempt and decay
 *       gradually after periods of normal behaviour.</li>
 *   <li>Successful solves reduce {@code consecutiveFailures} and contribute
 *       to a moving-average solve time.</li>
 *   <li>The "consecutive failure burst" used by the risk engine never goes
 *       negative and is capped at a small integer so a single bad day does
 *       not poison the score forever.</li>
 *   <li>Recovery events (admin reset / release after hold) are recorded so
 *       the audit timeline can show them.</li>
 * </ul>
 */
@Service
public class UserBehaviorProfileService {

    /** After this many minutes without a failure, recent-failure burst decays by 1. */
    static final long DECAY_MINUTES = 30;

    /** Hard cap on the burst counter. */
    static final int MAX_BURST = 6;

    private final UserBehaviorProfileRepository repository;

    public UserBehaviorProfileService(UserBehaviorProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserBehaviorProfile getOrCreate(User user) {
        return repository.findByUserId(user.getId())
                .orElseGet(() -> repository.save(new UserBehaviorProfile(user.getId())));
    }

    /**
     * Returns the burst-failure count adjusted for time decay. The risk engine
     * uses this number; the underlying counter is updated on the next write.
     */
    @Transactional(readOnly = true)
    public int recentFailureBurst(User user) {
        UserBehaviorProfile profile = repository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            return 0;
        }
        return decayedBurst(profile, LocalDateTime.now());
    }

    @Transactional
    public UserBehaviorProfile recordPuzzleSuccess(User user, long solveTimeMs) {
        UserBehaviorProfile profile = getOrCreate(user);
        profile.setPuzzleAttempts(profile.getPuzzleAttempts() + 1);
        profile.setPuzzleSuccesses(profile.getPuzzleSuccesses() + 1);
        profile.setConsecutiveFailures(Math.max(0, profile.getConsecutiveFailures() - 1));
        profile.setLastSuccessAt(LocalDateTime.now());
        profile.setAvgSolveTimeMs(updatedAverage(profile.getAvgSolveTimeMs(), profile.getPuzzleSuccesses(), solveTimeMs));
        profile.setLastUpdated(LocalDateTime.now());
        return repository.save(profile);
    }

    @Transactional
    public UserBehaviorProfile recordPuzzleFailure(User user) {
        UserBehaviorProfile profile = getOrCreate(user);
        profile.setPuzzleAttempts(profile.getPuzzleAttempts() + 1);
        profile.setPuzzleFailures(profile.getPuzzleFailures() + 1);
        profile.setConsecutiveFailures(Math.min(MAX_BURST, profile.getConsecutiveFailures() + 1));
        profile.setLastFailureAt(LocalDateTime.now());
        profile.setLastUpdated(LocalDateTime.now());
        return repository.save(profile);
    }

    @Transactional
    public UserBehaviorProfile recordRecoveryEvent(User user) {
        UserBehaviorProfile profile = getOrCreate(user);
        profile.setRecoveryEvents(profile.getRecoveryEvents() + 1);
        profile.setConsecutiveFailures(0);
        profile.setLastUpdated(LocalDateTime.now());
        return repository.save(profile);
    }

    @Transactional
    public UserBehaviorProfile resetCounters(User user) {
        UserBehaviorProfile profile = getOrCreate(user);
        profile.setConsecutiveFailures(0);
        profile.setLastUpdated(LocalDateTime.now());
        return repository.save(profile);
    }

    /**
     * Computes the burst that should be plugged into the adaptive risk score.
     * It walks backwards in time from "now" and removes one count per
     * {@link #DECAY_MINUTES} of quiet behaviour since the last failure.
     */
    static int decayedBurst(UserBehaviorProfile profile, LocalDateTime now) {
        int burst = profile.getConsecutiveFailures();
        if (burst <= 0) {
            return 0;
        }
        LocalDateTime reference = profile.getLastFailureAt() != null
                ? profile.getLastFailureAt()
                : profile.getLastUpdated();
        if (reference == null) {
            return burst;
        }
        long elapsed = Duration.between(reference, now).toMinutes();
        if (elapsed <= 0) {
            return burst;
        }
        long decay = elapsed / DECAY_MINUTES;
        if (decay <= 0) {
            return burst;
        }
        return Math.max(0, burst - (int) Math.min(decay, burst));
    }

    private static double updatedAverage(double previousAverage, long countAfter, long sample) {
        if (countAfter <= 1) {
            return Math.max(0, sample);
        }
        long countBefore = countAfter - 1;
        double sumBefore = previousAverage * countBefore;
        return (sumBefore + Math.max(0, sample)) / countAfter;
    }
}
