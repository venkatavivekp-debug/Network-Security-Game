package backend.util;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String AES = "AES";

    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void warmup() {
        secureRandom.nextBytes(new byte[1]);
    }

    public String encrypt(String plainText, byte[] keyBytes, int ivLengthBytes, String transformation, int gcmTagLengthBits) {
        try {
            byte[] iv = randomBytes(ivLengthBytes);
            Cipher cipher = Cipher.getInstance(transformation);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(gcmTagLengthBits, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(iv) + "." + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("Encryption operation failed", ex);
        }
    }

    public String decrypt(String token, byte[] keyBytes, String transformation, int gcmTagLengthBits) {
        try {
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted payload format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(transformation);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(gcmTagLengthBits, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Decryption operation failed", ex);
        }
    }

    public byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    public byte[] decodeBase64Key(String base64Key) {
        return Base64.getDecoder().decode(base64Key);
    }

    public String encodeBase64(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    public byte[] decodeBase64(String value) {
        return Base64.getDecoder().decode(value);
    }
}
