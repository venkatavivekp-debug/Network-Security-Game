package backend.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Computes and verifies lightweight request HMAC signatures.
 *
 * <p>Signature format: base64(HMAC_SHA256(secret, canonical_string)).
 */
public class RequestIntegrityService {

    public static String canonical(String method, String pathWithQuery, String body, String timestampMs, String nonce) {
        String m = method == null ? "" : method.toUpperCase();
        String p = pathWithQuery == null ? "" : pathWithQuery;
        String b = body == null ? "" : body;
        String ts = timestampMs == null ? "" : timestampMs;
        String n = nonce == null ? "" : nonce;
        return m + "\n" + p + "\n" + b + "\n" + ts + "\n" + n;
    }

    public static String signBase64(String secretB64, String canonicalString) {
        try {
            byte[] key = Base64.getDecoder().decode(secretB64);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] sig = mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig);
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC computation failed", ex);
        }
    }

    public static boolean verify(String secretB64, String canonicalString, String presentedB64) {
        if (secretB64 == null || canonicalString == null || presentedB64 == null) {
            return false;
        }
        String expected = signBase64(secretB64, canonicalString);
        return constantTimeEquals(expected, presentedB64);
    }

    static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aa = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aa, bb);
    }
}

