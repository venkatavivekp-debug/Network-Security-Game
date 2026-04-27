package backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived nonce store for replay protection.
 *
 * <p>Keys are session-scoped, so one user's nonce cannot collide with another's.
 * Values are expiry timestamps. The store is intentionally in-memory for a demo.
 */
@Service
public class ReplayNonceStore {

    private final Duration ttl;
    private final ConcurrentHashMap<String, Long> expiryByKey = new ConcurrentHashMap<>();

    public ReplayNonceStore(@Value("${app.security.replay.ttl-seconds:120}") long ttlSeconds) {
        this.ttl = Duration.ofSeconds(Math.max(30, Math.min(600, ttlSeconds)));
    }

    /** Returns true if stored; false if it already existed and is still valid. */
    public boolean storeIfNew(String sessionId, String nonce) {
        if (sessionId == null || nonce == null) return false;
        long now = System.currentTimeMillis();
        long exp = now + ttl.toMillis();
        String key = sessionId + ":" + nonce;
        Long prev = expiryByKey.putIfAbsent(key, exp);
        if (prev == null) {
            return true;
        }
        if (prev < now) {
            expiryByKey.replace(key, prev, exp);
            return true;
        }
        return false;
    }

    public Duration ttl() {
        return ttl;
    }

    /** Best-effort cleanup; cheap for the expected low key volume. */
    public void pruneExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = expiryByKey.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() != null && e.getValue() < now) {
                it.remove();
            }
        }
    }
}

