package backend.crypto;

import backend.exception.BadRequestException;
import backend.model.PuzzleType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lookup of {@link PuzzleEngine}s by {@link PuzzleType}. Spring discovers each
 * engine bean and the registry validates that every type has exactly one
 * implementation.
 */
@Component
public class PuzzleEngineRegistry {

    private final Map<PuzzleType, PuzzleEngine> engines;

    public PuzzleEngineRegistry(List<PuzzleEngine> beans) {
        Map<PuzzleType, PuzzleEngine> map = new EnumMap<>(PuzzleType.class);
        for (PuzzleEngine engine : beans) {
            PuzzleEngine existing = map.put(engine.type(), engine);
            if (existing != null) {
                throw new IllegalStateException("Duplicate puzzle engine for type " + engine.type());
            }
        }
        for (PuzzleType type : PuzzleType.values()) {
            if (!map.containsKey(type)) {
                throw new IllegalStateException("No puzzle engine registered for type " + type);
            }
        }
        this.engines = Map.copyOf(map);
    }

    public PuzzleEngine forType(PuzzleType type) {
        PuzzleEngine engine = engines.get(type);
        if (engine == null) {
            throw new BadRequestException("Unsupported puzzle type: " + type);
        }
        return engine;
    }

    public Set<PuzzleType> supportedTypes() {
        return engines.keySet();
    }
}
