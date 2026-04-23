package backend.config;

import backend.util.EncryptionUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Enforces that cryptographic keys are present for non-dev Spring profiles.
 *
 * Dev profile may use known placeholder keys for local demos, but docker/prod must supply real secrets.
 */
@Component
@Profile("!dev")
public class CryptoStartupValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoStartupValidator.class);

    private final CryptoProperties cryptoProperties;
    private final EncryptionUtil encryptionUtil;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    public CryptoStartupValidator(CryptoProperties cryptoProperties, EncryptionUtil encryptionUtil) {
        this.cryptoProperties = cryptoProperties;
        this.encryptionUtil = encryptionUtil;
    }

    @PostConstruct
    public void validate() {
        validateKey("app.crypto.master-key / APP_CRYPTO_MASTER_KEY", cryptoProperties.getMasterKey());
        validateKey("app.crypto.shcs-key / APP_CRYPTO_SHCS_KEY", cryptoProperties.getShcsKey());
        LOGGER.info("Crypto configuration validated for profiles: {}", activeProfiles);
    }

    private void validateKey(String label, String base64Key) {
        if (!StringUtils.hasText(base64Key)) {
            throw new IllegalStateException(label + " is required for this Spring profile");
        }
        try {
            byte[] decoded = encryptionUtil.decodeBase64Key(base64Key.trim());
            if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
                throw new IllegalStateException(label + " must decode to 16/24/32 bytes for AES (got " + decoded.length + ")");
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(label + " must be valid base64", ex);
        }
    }
}
