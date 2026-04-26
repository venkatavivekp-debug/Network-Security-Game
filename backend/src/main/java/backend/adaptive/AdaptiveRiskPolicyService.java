package backend.adaptive;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only view onto the adaptive engine's rule table.
 *
 * <p>The point of this service is transparency. The adaptive engine is a
 * weighted heuristic, not a Nash solver, and the admin/SOC console should be
 * able to see every threshold, every weight, and every action it takes so
 * operators can reason about (and disagree with) it. Nothing here is a magic
 * number that lives only inside one method.
 */
@Service
public class AdaptiveRiskPolicyService {

    private final AdaptiveSecurityProperties properties;

    public AdaptiveRiskPolicyService(AdaptiveSecurityProperties properties) {
        this.properties = properties;
    }

    /** Top-level structure suitable for direct JSON serialisation. */
    public Map<String, Object> describe() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("model", "weighted-heuristic");
        view.put("description",
                "Score = sum(weight_i * signal_i), clamped to [0,1]. Risk level is derived "
                        + "from the score and the configured thresholds. The engine does NOT solve a "
                        + "Nash equilibrium; it is a transparent rule table.");
        view.put("thresholds", thresholds());
        view.put("signals", signals());
        view.put("levelActions", levelActions());
        view.put("connectionStateContribution", connectionContribution());
        view.put("limitations", limitations());
        return view;
    }

    private Map<String, Object> thresholds() {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("elevated", properties.getElevatedThreshold());
        t.put("high", properties.getHighThreshold());
        t.put("critical", properties.getCriticalThreshold());
        t.put("lockMinutesOnCritical", properties.getLockMinutesOnCritical());
        return t;
    }

    private List<Map<String, Object>> signals() {
        return List.of(
                signal("failed_attempts",
                        properties.getWeightFailedAttempts(),
                        "Recent login failures and recent puzzle failures, normalised to [0,1]."),
                signal("unusual_login_time",
                        properties.getWeightUnusualLoginTime(),
                        "Hour-of-day delta vs. the user's last successful login (placeholder behavioural model)."),
                signal("new_device",
                        properties.getWeightNewDevice(),
                        "1.0 if the IP+User-Agent fingerprint changed since last login, otherwise 0."),
                signal("attack_intensity",
                        properties.getWeightAttackIntensity(),
                        "Admin-controlled global threat slider in [0,1]."),
                signal("behavior_deviation",
                        properties.getWeightBehaviorDeviation(),
                        "Burst of recent puzzle failures from this user.")
        );
    }

    private Map<String, Object> signal(String id, double weight, String explanation) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", id);
        entry.put("weight", weight);
        entry.put("explanation", explanation);
        return entry;
    }

    private List<Map<String, Object>> levelActions() {
        return List.of(
                action("LOW", "No step-up. Honour requested mode."),
                action("ELEVATED",
                        "Step requested NORMAL up to SHCS when threat intensity or score is high. Puzzle work factor mildly increases."),
                action("HIGH",
                        "Enforce CPHS and require a security challenge. Lower attempts and tighter time window."),
                action("CRITICAL",
                        "Enforce CPHS, hold the message for admin-supervised recovery. Account may be locked for a configurable window.")
        );
    }

    private Map<String, Object> action(String level, String summary) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("level", level);
        a.put("summary", summary);
        return a;
    }

    private Map<String, Object> connectionContribution() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("STABLE", "No reason added.");
        c.put("FIRST_SEEN", "No reason added; new fingerprint contributes via new_device signal.");
        c.put("SHIFTED", "Reason 'connection_shifted' is added; mode is not changed by the connection state alone.");
        c.put("ANOMALOUS",
                "Reason 'connection_anomalous' is added. If requested mode is NORMAL, mode is stepped up to SHCS. "
                        + "Connection state never enforces a hold by itself.");
        return c;
    }

    private List<String> limitations() {
        return List.of(
                "Heuristic, not a game-theoretic solver. Weights and thresholds are tunable but not learned.",
                "Fingerprint signals are network-layer hashes; they help adaptive risk but cannot prove device identity.",
                "Threat intensity is admin-driven; there is no live external threat feed wired in.",
                "All scoring happens server-side; no client-supplied scores are trusted."
        );
    }
}
