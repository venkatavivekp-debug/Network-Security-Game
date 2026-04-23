package backend.service;

import backend.config.CryptoProperties;
import backend.util.EncryptionUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CryptoServiceTest {

    @Test
    void shouldEncryptAndDecryptSuccessfully() {
        CryptoProperties properties = new CryptoProperties();
        properties.setMasterKey("aakXDomwwmk1+Q7QND5SRHiav1uWIQ8Y8XcLVfO9gLA=");
        properties.setIvLengthBytes(12);
        properties.setAesTransformation("AES/GCM/NoPadding");
        properties.setGcmTagLengthBits(128);

        EncryptionUtil encryptionUtil = new EncryptionUtil();
        encryptionUtil.warmup();

        CryptoService cryptoService = new CryptoService(properties, encryptionUtil);

        String plainText = "Phase4 encryption validation";
        String encrypted = cryptoService.encrypt(plainText);

        assertNotEquals(plainText, encrypted);
        assertEquals(plainText, cryptoService.decrypt(encrypted));
    }
}
