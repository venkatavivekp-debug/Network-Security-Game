package backend.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Per-session secret used for lightweight request integrity checks.
 *
 * <p>The secret is stored server-side in the {@link HttpSession}. The browser
 * receives a copy via {@code GET /security/integrity-key} so it can sign
 * sensitive actions (puzzle solve, decrypt, admin actions) with an HMAC.
 *
 * <p>This is not a replacement for TLS. It is a pragmatic anti-tamper and
 * anti-replay layer for cookie-bound session requests in a research demo.
 */
@Service
public class SessionIntegrityService {

    static final String SESSION_ATTR = "NSG_INTEGRITY_SECRET_B64";

    private final SecureRandom random = new SecureRandom();

    public String getOrCreateSecretB64(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object existing = session.getAttribute(SESSION_ATTR);
        if (existing instanceof String s && !s.isBlank()) {
            return s;
        }
        byte[] key = new byte[32];
        random.nextBytes(key);
        String b64 = Base64.getEncoder().encodeToString(key);
        session.setAttribute(SESSION_ATTR, b64);
        return b64;
    }
}

