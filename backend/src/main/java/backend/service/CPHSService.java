package backend.service;

import backend.adaptive.PuzzleDifficulty;
import backend.config.CphsProperties;
import backend.config.CryptoProperties;
import backend.crypto.CPHSDecryptionResult;
import backend.crypto.EncryptionPackage;
import backend.crypto.PuzzleEngine;
import backend.crypto.PuzzleEngineRegistry;
import backend.exception.BadRequestException;
import backend.model.PuzzleType;
import backend.util.EncryptionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class CPHSService {

    private final CphsProperties cphsProperties;
    private final CryptoProperties cryptoProperties;
    private final PuzzleEngineRegistry puzzleEngineRegistry;
    private final PuzzleService puzzleService;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;

    public CPHSService(
            CphsProperties cphsProperties,
            CryptoProperties cryptoProperties,
            PuzzleEngineRegistry puzzleEngineRegistry,
            PuzzleService puzzleService,
            EncryptionUtil encryptionUtil,
            ObjectMapper objectMapper
    ) {
        this.cphsProperties = cphsProperties;
        this.cryptoProperties = cryptoProperties;
        this.puzzleEngineRegistry = puzzleEngineRegistry;
        this.puzzleService = puzzleService;
        this.encryptionUtil = encryptionUtil;
        this.objectMapper = objectMapper;
    }

    public EncryptionPackage encryptWithPuzzle(String plainText) {
        return encryptWithPuzzle(plainText, PuzzleType.POW_SHA256, null);
    }

    public EncryptionPackage encryptWithPuzzle(String plainText, PuzzleDifficulty difficulty) {
        return encryptWithPuzzle(plainText, PuzzleType.POW_SHA256, difficulty);
    }

    public EncryptionPackage encryptWithPuzzle(String plainText, PuzzleType puzzleType, PuzzleDifficulty difficulty) {
        if (puzzleType == null) {
            puzzleType = PuzzleType.POW_SHA256;
        }
        byte[] randomMessageKey = encryptionUtil.randomBytes(cphsProperties.getRandomKeyBytes());

        String encryptedContent = encryptionUtil.encrypt(
                plainText,
                randomMessageKey,
                cryptoProperties.getIvLengthBytes(),
                cryptoProperties.getAesTransformation(),
                cryptoProperties.getGcmTagLengthBits()
        );

        PuzzleEngine engine = puzzleEngineRegistry.forType(puzzleType);
        PuzzleEngine.GeneratedPuzzle generated = engine.generate(randomMessageKey, difficulty);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("scheme", "CPHS");
        metadata.put("puzzleType", puzzleType.name());
        metadata.put("challenge", generated.challenge());
        metadata.put("targetHash", generated.targetHash());
        metadata.put("maxIterations", generated.maxIterations());
        metadata.put("wrappedKey", generated.wrappedKey());
        if (generated.metadata() != null) {
            metadata.putAll(generated.metadata());
            metadata.put("scheme", "CPHS");
            metadata.put("puzzleType", puzzleType.name());
        }
        return new EncryptionPackage(encryptedContent, toJson(metadata));
    }

    /** Decrypt after the receiver has solved a SHA-256 PoW puzzle (legacy path). */
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

    /**
     * Decrypt with a previously recovered raw key. Used for puzzle types whose
     * answer is not numeric, where {@link Puzzle#getRecoveredKey()} stores the
     * unwrapped key after a successful solve.
     */
    public CPHSDecryptionResult decryptWithRecoveredKey(String encryptedContent, String recoveredKeyBase64) {
        if (recoveredKeyBase64 == null || recoveredKeyBase64.isBlank()) {
            throw new BadRequestException("Recovered key is missing for this puzzle");
        }
        byte[] recoveredKey = Base64.getDecoder().decode(recoveredKeyBase64);
        long start = System.currentTimeMillis();
        String plainText = encryptionUtil.decrypt(
                encryptedContent,
                recoveredKey,
                cryptoProperties.getAesTransformation(),
                cryptoProperties.getGcmTagLengthBits()
        );
        return new CPHSDecryptionResult(plainText, System.currentTimeMillis() - start);
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
