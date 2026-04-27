package backend.security;

import backend.adaptive.UserBehaviorProfileService;
import backend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Lightweight anomaly-driven throttling for sensitive actions.
 *
 * <p>The goal is to slow down active attackers (brute forcing puzzles, rapid
 * decrypt attempts, scripted admin requests) without permanently blocking the
 * user. All paths keep a recovery route.
 */
@Service
public class AdaptiveThrottleService {

    private final UserBehaviorProfileService behavior;
    private final long minGapMs;
    private final long maxDelayMs;

    private final ConcurrentMap<String, Long> lastSensitiveAtMs = new ConcurrentHashMap<>();

    public AdaptiveThrottleService(
            UserBehaviorProfileService behavior,
            @Value("${app.security.throttle.min-gap-ms:180}") long minGapMs,
            @Value("${app.security.throttle.max-delay-ms:900}") long maxDelayMs
    ) {
        this.behavior = behavior;
        this.minGapMs = Math.max(50, Math.min(2000, minGapMs));
        this.maxDelayMs = Math.max(100, Math.min(5000, maxDelayMs));
    }

    /**
     * Returns a delay to apply in milliseconds (0 means no throttle).
     *
     * <p>Inputs:
     * - rapid repeated sensitive requests (tight min-gap)
     * - elevated consecutive failure burst (puzzle brute force)
     */
    public long computeDelayMs(User user, String actionKey) {
        if (user == null) return 0;
        String key = user.getUsername() + "|" + (actionKey == null ? "" : actionKey);
        long now = System.currentTimeMillis();
        Long last = lastSensitiveAtMs.put(key, now);

        long delay = 0;
        if (last != null) {
            long delta = now - last;
            if (delta >= 0 && delta < minGapMs) {
                delay = Math.max(delay, minGapMs - delta);
            }
        }

        int burst = behavior.recentFailureBurst(user);
        if (burst >= 4) {
            delay = Math.max(delay, 250L + (burst - 3) * 150L);
        }

        return Math.min(maxDelayMs, Math.max(0, delay));
    }
}

