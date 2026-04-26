package backend.security;

import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.model.User;
import backend.util.HashUtil;
import backend.util.RequestContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Layered evaluator for session/device consistency.
 *
 * <p>Looks at multiple opaque signals (IP, User-Agent, Accept-Language, session
 * id) and tracks a small per-user snapshot in memory. The result is one of
 * {@link ConnectionSecurityState#FIRST_SEEN}, {@link ConnectionSecurityState#STABLE},
 * {@link ConnectionSecurityState#SHIFTED} (single signal changed) or
 * {@link ConnectionSecurityState#ANOMALOUS} (multiple signals changed at once,
 * or the connection has accumulated repeated shifts).
 *
 * <p>By design this service <em>does not block</em> users on a fingerprint
 * mismatch. It feeds the adaptive risk score (so a SHIFTED/ANOMALOUS connection
 * adds risk that may step up the puzzle) and writes an audit event so the SOC
 * can investigate. Cryptographic key derivation never depends on the
 * fingerprint.
 */
@Service
public class ConnectionSecurityService {

    /** Threshold of accumulated shifts after which a session is treated as anomalous. */
    private static final int REPEAT_SHIFT_ANOMALY_THRESHOLD = 3;

    /** Hard cap on the in-memory snapshot map. Crude eviction beyond this. */
    private static final int MAX_SNAPSHOTS = 10_000;

    public enum ConnectionSecurityState {
        STABLE,
        FIRST_SEEN,
        SHIFTED,
        ANOMALOUS
    }

    /**
     * Evaluation surface returned to callers. {@code fingerprintHash} is a
     * composite hash kept for backward compatibility with the previous
     * single-signal model; new callers should prefer {@link #snapshot}.
     */
    public record Evaluation(
            ConnectionSecurityState state,
            String fingerprintHash,
            ConnectionFingerprintSnapshot snapshot,
            List<String> shiftedSignals
    ) {
    }

    private final HashUtil hashUtil;
    private final AuditService auditService;
    private final RequestContextUtil requestContextUtil;
    private final ConcurrentMap<String, ConnectionFingerprintSnapshot> snapshots = new ConcurrentHashMap<>();

    public ConnectionSecurityService(HashUtil hashUtil, AuditService auditService, RequestContextUtil requestContextUtil) {
        this.hashUtil = hashUtil;
        this.auditService = auditService;
        this.requestContextUtil = requestContextUtil;
    }

    /** Convenience: pull all signals from the current servlet request. */
    public Evaluation evaluate(User user, HttpServletRequest request) {
        String ip = requestContextUtil.clientIp(request);
        String ua = requestContextUtil.userAgent(request);
        String acceptLang = request == null ? null : request.getHeader("Accept-Language");
        String sessionId = (request == null || request.getSession(false) == null)
                ? null
                : request.getSession(false).getId();
        return evaluate(user, ip, ua, acceptLang, sessionId);
    }

    /** Backward-compatible 2-signal call kept so existing callers keep working. */
    public Evaluation evaluate(User user, String ip, String userAgent) {
        return evaluate(user, ip, userAgent, null, null);
    }

    /**
     * Layered evaluation. Each signal is hashed independently; the snapshot
     * keeps {@code firstSeen}, {@code lastSeen}, and an accumulating
     * {@code anomalyCount}.
     */
    public Evaluation evaluate(User user, String ip, String userAgent, String acceptLanguage, String sessionId) {
        String ipHash = optionalHash(ip);
        String uaHash = optionalHash(userAgent);
        String langHash = optionalHash(acceptLanguage);
        String sessionHash = optionalHash(sessionId);
        String composite = compositeFingerprint(ipHash, uaHash);

        if (user == null) {
            ConnectionFingerprintSnapshot anonymous = new ConnectionFingerprintSnapshot(
                    ipHash, uaHash, langHash, sessionHash, LocalDateTime.now(), LocalDateTime.now(), 0);
            return new Evaluation(ConnectionSecurityState.FIRST_SEEN, composite, anonymous, List.of());
        }

        String key = user.getUsername();
        ConnectionFingerprintSnapshot previous = snapshots.get(key);
        boolean firstSeen = previous == null && (user.getLastLoginFingerprintHash() == null
                || user.getLastLoginFingerprintHash().isBlank());

        ConnectionSecurityState state;
        List<String> shifted = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int newAnomalyCount = previous == null ? 0 : previous.anomalyCount();
        LocalDateTime firstSeenAt = previous == null ? now : previous.firstSeen();

        if (firstSeen) {
            state = ConnectionSecurityState.FIRST_SEEN;
        } else if (previous != null) {
            shifted = diffSignals(previous, ipHash, uaHash, langHash, sessionHash);
            if (shifted.isEmpty()) {
                state = ConnectionSecurityState.STABLE;
            } else if (shifted.size() == 1 && newAnomalyCount + 1 < REPEAT_SHIFT_ANOMALY_THRESHOLD) {
                state = ConnectionSecurityState.SHIFTED;
                newAnomalyCount += 1;
            } else {
                state = ConnectionSecurityState.ANOMALOUS;
                newAnomalyCount += 1;
            }
        } else {
            // No in-memory snapshot but user has a stored single-signal fingerprint:
            // treat as a soft check against ip+ua only (legacy fallback).
            String legacy = user.getLastLoginFingerprintHash();
            if (legacy != null && legacy.equals(composite)) {
                state = ConnectionSecurityState.STABLE;
            } else if (legacy != null) {
                state = ConnectionSecurityState.SHIFTED;
                shifted.add("composite");
                newAnomalyCount += 1;
            } else {
                state = ConnectionSecurityState.FIRST_SEEN;
            }
        }

        ConnectionFingerprintSnapshot updated = new ConnectionFingerprintSnapshot(
                ipHash, uaHash, langHash, sessionHash, firstSeenAt, now, newAnomalyCount);
        if (snapshots.size() >= MAX_SNAPSHOTS) {
            snapshots.clear();
        }
        snapshots.put(key, updated);

        if (state == ConnectionSecurityState.ANOMALOUS) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("shiftedSignals", shifted);
            details.put("anomalyCount", newAnomalyCount);
            details.put("snapshot", updated.toView());
            auditService.record(
                    AuditEventType.SESSION_ANOMALY,
                    user.getUsername(),
                    user.getUsername(),
                    ip,
                    userAgent,
                    null,
                    details
            );
        }

        return new Evaluation(state, composite, updated, List.copyOf(shifted));
    }

    /** Test/admin helper: reset the per-user snapshot. */
    public void forget(String username) {
        if (username != null) {
            snapshots.remove(username);
        }
    }

    /** Read-only view of the current snapshot, if any. */
    public ConnectionFingerprintSnapshot peek(String username) {
        return username == null ? null : snapshots.get(username);
    }

    private String optionalHash(String input) {
        if (input == null || input.isBlank()) return null;
        return hashUtil.sha256Hex(input);
    }

    private String compositeFingerprint(String ipHash, String uaHash) {
        return hashUtil.sha256Hex((ipHash == null ? "" : ipHash) + "|" + (uaHash == null ? "" : uaHash));
    }

    private List<String> diffSignals(
            ConnectionFingerprintSnapshot previous,
            String ipHash, String uaHash, String langHash, String sessionHash
    ) {
        List<String> shifted = new ArrayList<>();
        if (changed(previous.ipHash(), ipHash)) shifted.add("ip");
        if (changed(previous.userAgentHash(), uaHash)) shifted.add("user_agent");
        if (changed(previous.acceptLanguageHash(), langHash)) shifted.add("accept_language");
        if (changed(previous.sessionIdHash(), sessionHash)) shifted.add("session_id");
        return shifted;
    }

    private boolean changed(String previous, String current) {
        if (previous == null || current == null) return false;
        return !previous.equals(current);
    }
}
