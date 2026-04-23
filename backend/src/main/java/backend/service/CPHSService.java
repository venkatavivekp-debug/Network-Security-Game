package backend.service;

import backend.config.CphsProperties;
import backend.config.CryptoProperties;
import backend.crypto.CPHSDecryptionResult;
import backend.crypto.EncryptionPackage;
import backend.crypto.PuzzleDescriptor;
import backend.exception.BadRequestException;
import backend.util.EncryptionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CPHSService {

    private final CphsProperties cphsProperties;
    private final CryptoProperties cryptoProperties;
    private final PuzzleService puzzleService;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;

    public CPHSService(
            CphsProperties cphsProperties,
            CryptoProperties cryptoProperties,
            PuzzleService puzzleService,
            EncryptionUtil encryptionUtil,
            ObjectMapper objectMapper
    ) {
        this.cphsProperties = cphsProperties;
        this.cryptoProperties = cryptoProperties;
        this.puzzleService = puzzleService;
        this.encryptionUtil = encryptionUtil;
        this.objectMapper = objectMapper;
    }

    public EncryptionPackage encryptWithPuzzle(String plainText) {
        return encryptWithPuzzle(plainText, cphsProperties.getRandomKeyBytes(), null);
    }

    public EncryptionPackage encryptWithPuzzle(String plainText, Integer overrideMaxIterations) {
        return encryptWithPuzzle(plainText, cphsProperties.getRandomKeyBytes(), overrideMaxIterations);
    }

    private EncryptionPackage encryptWithPuzzle(String plainText, int randomKeyBytes, Integer overrideMaxIterations) {
        byte[] randomMessageKey = encryptionUtil.randomBytes(randomKeyBytes);

        String encryptedContent = encryptionUtil.encrypt(
                plainText,
                randomMessageKey,
                cryptoProperties.getIvLengthBytes(),
                cryptoProperties.getAesTransformation(),
                cryptoProperties.getGcmTagLengthBits()
        );

        PuzzleService.PuzzlePackagingResult packagingResult = overrideMaxIterations == null
                ? puzzleService.createPuzzlePackage(randomMessageKey)
                : puzzleService.createPuzzlePackage(randomMessageKey, overrideMaxIterations);
        PuzzleDescriptor descriptor = packagingResult.getDescriptor();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("scheme", "CPHS");
        metadata.put("challenge", descriptor.getChallenge());
        metadata.put("targetHash", descriptor.getTargetHash());
        metadata.put("maxIterations", descriptor.getMaxIterations());
        metadata.put("wrappedKey", packagingResult.getWrappedKeyBase64());

        return new EncryptionPackage(encryptedContent, toJson(metadata));
    }

    public CPHSDecryptionResult decryptAfterPuzzleSolve(String encryptedContent, String metadataJson) {
        Map<String, Object> metadata = fromJson(metadataJson);

        String challenge = requiredString(metadata, "challenge");
        String targetHash = requiredString(metadata, "targetHash");
        int maxIterations = requiredInteger(metadata, "maxIterations");
        String wrappedKey = requiredString(metadata, "wrappedKey");

        PuzzleService.PuzzleSolveResult solveResult = puzzleService.solveAndRecoverKey(
                challenge,
                targetHash,
                maxIterations,
                wrappedKey
        );

        String plainText = encryptionUtil.decrypt(
                encryptedContent,
                solveResult.getRecoveredKey(),
                cryptoProperties.getAesTransformation(),
                cryptoProperties.getGcmTagLengthBits()
        );

        return new CPHSDecryptionResult(plainText, solveResult.getSolveTimeMs());
    }

    public CPHSDecryptionResult decryptAfterPuzzleNonce(String encryptedContent, String metadataJson, int nonce) {
        Map<String, Object> metadata = fromJson(metadataJson);

        String challenge = requiredString(metadata, "challenge");
        String targetHash = requiredString(metadata, "targetHash");
        int maxIterations = requiredInteger(metadata, "maxIterations");
        String wrappedKey = requiredString(metadata, "wrappedKey");

        if (nonce < 0 || nonce >= maxIterations) {
            throw new BadRequestException("nonce must be within puzzle iteration bounds");
        }

        long start = System.currentTimeMillis();
        byte[] recoveredKey = puzzleService.recoverKeyFromNonce(challenge, targetHash, nonce, wrappedKey);

        String plainText = encryptionUtil.decrypt(
                encryptedContent,
                recoveredKey,
                cryptoProperties.getAesTransformation(),
                cryptoProperties.getGcmTagLengthBits()
        );

        long totalTime = System.currentTimeMillis() - start;
        return new CPHSDecryptionResult(plainText, totalTime);
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new BadRequestException("Invalid CPHS metadata format");
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize CPHS metadata", ex);
        }
    }

    private String requiredString(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new BadRequestException("Missing metadata field: " + key);
        }
        return value.toString();
    }

    private int requiredInteger(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            throw new BadRequestException("Missing metadata field: " + key);
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid numeric metadata field: " + key);
        }
    }
}
