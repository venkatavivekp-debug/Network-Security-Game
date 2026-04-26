package backend.security.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory backend for the rate limiter.
 *
 * <p>Holds a {@link ConcurrentHashMap} of {@link TokenBucket} per key. This is
 * the right choice for single-process deployments (local dev, the research
 * demo, and small docker installs). For production behind multiple replicas a
 * shared backend should be wired instead -- see {@link RateLimiterBackend} for
 * the contract.
 *
 * <p>The bean is marked {@code @Primary} and {@code ConditionalOnMissingBean}
 * so an explicit alternative implementation provided by the deployer wins.
 */
@Component
@Primary
@ConditionalOnMissingBean(name = "rateLimiterBackend")
@ConditionalOnProperty(prefix = "app.ratelimit", name = "backend", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimiterBackend implements RateLimiterBackend {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String key, long capacity, double refillPerSecond) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillPerSecond));
        return bucket.tryConsume(1);
    }

    @Override
    public long retryAfterSeconds(String key, long capacity, double refillPerSecond) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillPerSecond));
        return Math.max(1, bucket.retryAfterSeconds(1));
    }
}
