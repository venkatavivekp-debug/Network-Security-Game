package backend.security.ratelimit;

import org.springframework.stereotype.Service;

/**
 * Thin facade over a {@link RateLimiterBackend}. The filter calls
 * {@link #checkOrThrow} and the backend decides whether to allow the request.
 *
 * <p>The default backend is {@link InMemoryRateLimiterBackend}; production
 * deployments may provide a shared-store backend (e.g. Redis) by wiring an
 * alternative {@link RateLimiterBackend} bean.
 */
@Service
public class RateLimiterService {

    private final RateLimiterBackend backend;

    public RateLimiterService(RateLimiterBackend backend) {
        this.backend = backend;
    }

    public void checkOrThrow(String key, long capacity, double refillPerSecond) {
        if (backend.tryConsume(key, capacity, refillPerSecond)) {
            return;
        }
        long retry = backend.retryAfterSeconds(key, capacity, refillPerSecond);
        throw new RateLimitExceededException("Too many requests", Math.max(1, retry));
    }

    /** Exposed for tests/debug; not part of the request flow. */
    public RateLimiterBackend backend() {
        return backend;
    }
}
