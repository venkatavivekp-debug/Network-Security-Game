package backend.security.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rate limiter is decoupled from its storage via {@link RateLimiterBackend};
 * this test runs the same flow against the default in-memory backend and a
 * stub backend to confirm the swap is clean.
 */
class RateLimiterBackendTest {

    @Test
    void inMemoryBackendAllowsBurstWithinCapacity() {
        InMemoryRateLimiterBackend backend = new InMemoryRateLimiterBackend();
        for (int i = 0; i < 5; i++) {
            assertTrue(backend.tryConsume("user-A", 5, 1.0));
        }
        assertFalse(backend.tryConsume("user-A", 5, 1.0));
        assertTrue(backend.retryAfterSeconds("user-A", 5, 1.0) >= 1);
    }

    @Test
    void rateLimiterServiceWorksAgainstAlternativeBackend() {
        AtomicInteger consumeCalls = new AtomicInteger();
        RateLimiterBackend stub = new RateLimiterBackend() {
            @Override
            public boolean tryConsume(String key, long capacity, double refillPerSecond) {
                consumeCalls.incrementAndGet();
                return consumeCalls.get() < 3;
            }

            @Override
            public long retryAfterSeconds(String key, long capacity, double refillPerSecond) {
                return 7;
            }
        };

        RateLimiterService service = new RateLimiterService(stub);
        assertDoesNotThrow(() -> service.checkOrThrow("k", 10, 1.0));
        assertDoesNotThrow(() -> service.checkOrThrow("k", 10, 1.0));
        RateLimitExceededException ex = assertThrows(
                RateLimitExceededException.class,
                () -> service.checkOrThrow("k", 10, 1.0)
        );
        assertEquals(7, ex.getRetryAfterSeconds());
    }
}
