package backend.security.ratelimit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimiterServiceTest {

    @Test
    void shouldThrowWhenLimitExceeded() {
        RateLimiterService service = new RateLimiterService();
        String key = "k1";

        // capacity 1, no refill in test window
        service.checkOrThrow(key, 1, 0.0001);
        assertThrows(RateLimitExceededException.class, () -> service.checkOrThrow(key, 1, 0.0001));
    }
}

