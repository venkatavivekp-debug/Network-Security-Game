package backend.service;

import backend.config.CryptoProperties;
import backend.crypto.EncryptionPackage;
import backend.exception.BadRequestException;
import backend.util.EncryptionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class SHCSService {

    private final CryptoProperties cryptoProperties;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;

    public SHCSService(CryptoProperties cryptoProperties, EncryptionUtil encryptionUtil, ObjectMapper objectMapper) {
        this.cryptoProperties = cryptoProperties;
        this.encryptionUtil = encryptionUtil;
        this.objectMapper = objectMapper;
    }

    public EncryptionPackage encryptAndHideHeader(String plainText, String senderUsername, String receiverUsername) {
        byte[] key = encryptionUtil.decodeBase64Key(cryptoProperties.getShcsKey());

        String innerCipher = encryptionUtil.encrypt(
                plainText,
                key,
                cryptoProperties.getIvLengthBytes(),
                cryptoProperties.getAesTransformation(),
                cryptoProperties.getGcmTagLengthBits()
        );

        String hiddenHeader = buildHiddenHeader(senderUsername, receiverUsername);
        String embeddedPayload = Base64.getEncoder().encodeToString(hiddenHeader.getBytes()) + "|" + innerCipher;

        String outerCipher = encryptionUtil.encrypt(
                embeddedPayload,
                key,
                cryptoProperties.getIvLengthBytes(),
                cryptoProperties.getAesTransformation(),
                cryptoProperties.getGcmTagLengthBits()
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("visibility", "MINIMIZED");
        metadata.put("profile", "SHCS");
        metadata.put("transport_hint", "opaque-packet");

        return new EncryptionPackage(outerCipher, writeJson(metadata));
    }

    public String decryptAndExtract(String encryptedContent) {
        byte[] key = encryptionUtil.decodeBase64Key(cryptoProperties.getShcsKey());

        String embeddedPayload = encryptionUtil.decrypt(
                encryptedContent,
                key,
                cryptoProperties.getAesTransformation(),
                cryptoProperties.getGcmTagLengthBits()
        );

        String[] payloadParts = embeddedPayload.split("\\|", 2);
        if (payloadParts.length != 2) {
            throw new BadRequestException("Invalid SHCS payload format");
        }

        String innerCipher = payloadParts[1];

        return encryptionUtil.decrypt(
                innerCipher,
                key,
                cryptoProperties.getAesTransformation(),
                cryptoProperties.getGcmTagLengthBits()
        );
    }

    private String buildHiddenHeader(String senderUsername, String receiverUsername) {
        Map<String, Object> header = new HashMap<>();
        header.put("sender", senderUsername);
        header.put("receiver", receiverUsername);
        header.put("timestamp", LocalDateTime.now().toString());
        header.put("scheme", "SHCS-HIDDEN");
        return writeJson(header);
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON serialization failed", ex);
        }
    }
}
