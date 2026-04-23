package backend.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class HashUtil {

    public String sha256Hex(String value) {
        byte[] digest = sha256Bytes(value);
        return HexFormat.of().formatHex(digest);
    }

    public byte[] sha256Bytes(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Hash computation failed", ex);
        }
    }
}
