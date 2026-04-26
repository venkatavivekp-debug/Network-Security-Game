package backend.security;

import backend.adaptive.AdaptiveSecurityService;
import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.model.User;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Evaluates session/device consistency at the edge of the secure-connection model.
 *
 * <p>This service is intentionally small. It does <em>not</em> bind cryptographic
 * material to the fingerprint (that would change the threat model and core flow);
 * it only computes a {@link ConnectionSecurityState} from the current request
 * fingerprint vs. what was recorded at last login, and surfaces an audit event
 * when the connection looks suspicious. The adaptive engine is the system that
 * decides what to enforce based on the resulting risk score.
 */
@Service
public class ConnectionSecurityService {

    /** Stable / anomalous / never-seen-this-fingerprint states. */
    public enum ConnectionSecurityState {
        STABLE,
        FIRST_SEEN,
        ANOMALOUS
    }

    public record Evaluation(ConnectionSecurityState state, String fingerprintHash) {
    }

    private final AdaptiveSecurityService adaptiveSecurityService;
    private final AuditService auditService;

    public ConnectionSecurityService(AdaptiveSecurityService adaptiveSecurityService, AuditService auditService) {
        this.adaptiveSecurityService = adaptiveSecurityService;
        this.auditService = auditService;
    }

    /**
     * Compute the connection security state for a user given the current request's
     * IP and User-Agent. Emits a {@link AuditEventType#SESSION_ANOMALY} audit event
     * when the fingerprint differs from the one recorded on the last successful login.
     */
    public Evaluation evaluate(User user, String ip, String userAgent) {
        String currentFp = adaptiveSecurityService.fingerprint(ip, userAgent);
        String lastFp = user == null ? null : user.getLastLoginFingerprintHash();

        ConnectionSecurityState state;
        if (lastFp == null || lastFp.isBlank()) {
            state = ConnectionSecurityState.FIRST_SEEN;
        } else if (lastFp.equals(currentFp)) {
            state = ConnectionSecurityState.STABLE;
        } else {
            state = ConnectionSecurityState.ANOMALOUS;
        }

        if (state == ConnectionSecurityState.ANOMALOUS && user != null) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("knownFingerprint", lastFp);
            details.put("seenFingerprint", currentFp);
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

        return new Evaluation(state, currentFp);
    }
}
