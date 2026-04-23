package backend.simulation.advanced;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Random;

@Component
public class RandomStrategySupport {

    public long resolveSeed(Long requestedSeed, long deterministicFallback) {
        if (requestedSeed != null) {
            return requestedSeed;
        }
        return deterministicFallback;
    }

    public Random resolveRandom(Long requestedSeed, long deterministicFallback) {
        return new Random(resolveSeed(requestedSeed, deterministicFallback));
    }

    public Random deriveRandom(long seed, String scope, int round) {
        return new Random(Objects.hash(seed, scope, round));
    }

    public boolean chance(Random random, double probability) {
        return random.nextDouble() <= clamp(probability);
    }

    public int boundedInt(Random random, int min, int maxInclusive) {
        if (maxInclusive <= min) {
            return min;
        }
        return min + random.nextInt(maxInclusive - min + 1);
    }

    public double boundedDouble(Random random, double min, double max) {
        if (max <= min) {
            return min;
        }
        return min + (max - min) * random.nextDouble();
    }

    public double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        return Math.min(value, 1.0);
    }
}
