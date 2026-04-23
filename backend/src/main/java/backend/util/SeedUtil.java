package backend.util;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Small deterministic seed derivation helpers.
 *
 * These are not cryptographic RNGs; they only exist to make simulations reproducible
 * when the user does not explicitly provide a seed.
 */
public final class SeedUtil {

    private SeedUtil() {
    }

    public static long mix64(long seed, String scope, int salt) {
        long h = seed ^ Objects.hash(scope, salt);
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return h & Long.MAX_VALUE; // keep non-negative for APIs that require it
    }

    public static long stableHash64(String... parts) {
        long h = 0x243f6a8885a308d3L;
        for (String part : parts) {
            byte[] bytes = (part == null ? "" : part).getBytes(StandardCharsets.UTF_8);
            for (byte b : bytes) {
                h = (h * 31L) + (b & 0xffL);
                h ^= (h >>> 27);
                h *= 0x85ebca6bL;
                h ^= (h >>> 13);
            }
        }
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        return h & Long.MAX_VALUE;
    }
}
