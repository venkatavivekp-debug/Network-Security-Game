package backend.crypto;

import backend.config.PuzzleProperties;
import backend.util.HashUtil;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Shared key-wrapping helper for puzzle types whose solution is a
 * canonical answer string (arithmetic, encoded, pattern). Producing the same
 * derivation at solve time yields the AES message key that the receiver needs
 * to decrypt the message.
 *
 * <p>Concretely:
 * <pre>
 *   targetHash  = SHA-256(challenge + ':' + canonicalAnswer)
 *   keyMaterial = SHA-256(challenge + ':' + canonicalAnswer + ':' + salt)
 *   wrappedKey  = messageKey XOR keyMaterial
 * </pre>
 *
 * <p>This mirrors {@link backend.service.PuzzleService}'s POW wrap, so the
 * cryptographic envelope is uniform across all puzzle types.
 */
@Component
public class AnswerKeyDerivation {

    private final HashUtil hashUtil;
    private final PuzzleProperties puzzleProperties;

    public AnswerKeyDerivation(HashUtil hashUtil, PuzzleProperties puzzleProperties) {
        this.hashUtil = hashUtil;
        this.puzzleProperties = puzzleProperties;
    }

    public String targetHash(String challenge, String canonicalAnswer) {
        return hashUtil.sha256Hex(challenge + ":" + canonicalAnswer);
    }

    public byte[] derive(String challenge, String canonicalAnswer) {
        String material = challenge + ":" + canonicalAnswer + ":" + puzzleProperties.getKeyDerivationSalt();
        return hashUtil.sha256Bytes(material);
    }

    public String wrapKey(String challenge, String canonicalAnswer, byte[] messageKey) {
        byte[] derived = derive(challenge, canonicalAnswer);
        byte[] wrapped = xor(messageKey, derived);
        return Base64.getEncoder().encodeToString(wrapped);
    }

    public byte[] unwrapKey(String challenge, String canonicalAnswer, String wrappedKeyBase64) {
        byte[] derived = derive(challenge, canonicalAnswer);
        byte[] wrapped = Base64.getDecoder().decode(wrappedKeyBase64);
        return xor(wrapped, derived);
    }

    public String answerHash(String canonicalAnswer) {
        return hashUtil.sha256Hex(canonicalAnswer);
    }

    private byte[] xor(byte[] left, byte[] right) {
        byte[] result = new byte[left.length];
        for (int i = 0; i < left.length; i++) {
            result[i] = (byte) (left[i] ^ right[i % right.length]);
        }
        return result;
    }
}
