package backend.service;

import backend.config.CryptoProperties;
import backend.util.EncryptionUtil;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

    private final CryptoProperties cryptoProperties;
    private final EncryptionUtil encryptionUtil;

    public CryptoService(CryptoProperties cryptoProperties, EncryptionUtil encryptionUtil) {
        this.cryptoProperties = cryptoProperties;
        this.encryptionUtil = encryptionUtil;
    }

    public String encrypt(String plainText) {
        byte[] masterKey = encryptionUtil.decodeBase64Key(cryptoProperties.getMasterKey());
        return encryptionUtil.encrypt(
                plainText,
                masterKey,
                cryptoProperties.getIvLengthBytes(),
                cryptoProperties.getAesTransformation(),
                cryptoProperties.getGcmTagLengthBits()
        );
    }

    public String decrypt(String encryptedContent) {
        byte[] masterKey = encryptionUtil.decodeBase64Key(cryptoProperties.getMasterKey());
        return encryptionUtil.decrypt(
                encryptedContent,
                masterKey,
                cryptoProperties.getAesTransformation(),
                cryptoProperties.getGcmTagLengthBits()
        );
    }
}
