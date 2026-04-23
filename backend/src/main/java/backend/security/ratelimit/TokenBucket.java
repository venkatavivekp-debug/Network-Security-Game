package backend.security.ratelimit;

public class TokenBucket {

    private final long capacity;
    private final double refillPerSecond;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(long capacity, double refillPerSecond) {
        this.capacity = Math.max(1, capacity);
        this.refillPerSecond = Math.max(0.0001, refillPerSecond);
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    public synchronized boolean tryConsume(long n) {
        refill();
        if (tokens >= n) {
            tokens -= n;
            return true;
        }
        return false;
    }

    public synchronized long retryAfterSeconds(long n) {
        refill();
        if (tokens >= n) return 0;
        double missing = n - tokens;
        double seconds = missing / refillPerSecond;
        return (long) Math.ceil(seconds);
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        if (elapsed <= 0) return;
        double add = (elapsed / 1_000_000_000.0) * refillPerSecond;
        tokens = Math.min(capacity, tokens + add);
        lastRefillNanos = now;
    }
}

