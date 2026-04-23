package backend.security.ratelimit;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public void checkOrThrow(String key, long capacity, double refillPerSecond) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillPerSecond));
        if (bucket.tryConsume(1)) {
            return;
        }
        long retry = bucket.retryAfterSeconds(1);
        throw new RateLimitExceededException("Too many requests", Math.max(1, retry));
    }
}

