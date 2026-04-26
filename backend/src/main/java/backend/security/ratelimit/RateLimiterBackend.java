package backend.security.ratelimit;

/**
 * Storage abstraction for rate-limiter buckets so the bucket store can be
 * swapped without touching the filter or the service that decides limits.
 *
 * <p>Two implementations are intended:
 * <ul>
 *   <li>{@link InMemoryRateLimiterBackend} -- the default; per-process token
 *       buckets in a {@code ConcurrentHashMap}. Good for local development,
 *       single-instance deployments, and the research demo.</li>
 *   <li>A future {@code RedisRateLimiterBackend} -- not implemented in this
 *       repo, but the contract is intentionally trivial (try-consume + retry
 *       hint per key) so it can be added by writing one class that talks to
 *       a shared store. Production deployments behind multiple replicas
 *       should swap to that backend so per-IP/user limits stay accurate
 *       across instances.</li>
 * </ul>
 *
 * <p>Implementations must be thread-safe. {@code retryAfterSeconds} is allowed
 * to be a best-effort estimate; the filter only uses it for the {@code Retry-After}
 * header.
 */
public interface RateLimiterBackend {

    /**
     * Try to consume a single token from the bucket identified by {@code key}.
     * Returns {@code true} when allowed, {@code false} when the bucket is
     * empty.
     *
     * @param key opaque limiter key (already includes IP/user/path scope)
     * @param capacity bucket capacity
     * @param refillPerSecond refill rate in tokens per second
     */
    boolean tryConsume(String key, long capacity, double refillPerSecond);

    /**
     * Best-effort hint, in seconds, for how long the caller should wait before
     * retrying. Implementations may return {@code 1} as a safe minimum.
     */
    long retryAfterSeconds(String key, long capacity, double refillPerSecond);
}
