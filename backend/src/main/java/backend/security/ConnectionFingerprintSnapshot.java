package backend.security;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Layered, opaque snapshot of the network signals we keep about a session.
 *
 * <p>Each individual signal is hashed so the snapshot itself never holds raw
 * IPs, user-agents, or session ids. The snapshot is used only as input to the
 * adaptive risk score and the audit trail. It is intentionally <em>not</em>
 * bound into key derivation: a stable laptop on a moving network is a normal
 * user, not an attacker.
 */
public record ConnectionFingerprintSnapshot(
        String ipHash,
        String userAgentHash,
        String acceptLanguageHash,
        String sessionIdHash,
        LocalDateTime firstSeen,
        LocalDateTime lastSeen,
        int anomalyCount
) {

    /** Convenience for serialization in admin/auth responses (already opaque). */
    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("ipHash", ipHash);
        view.put("userAgentHash", userAgentHash);
        view.put("acceptLanguageHash", acceptLanguageHash);
        view.put("sessionIdHash", sessionIdHash);
        view.put("firstSeen", firstSeen);
        view.put("lastSeen", lastSeen);
        view.put("anomalyCount", anomalyCount);
        return view;
    }
}
