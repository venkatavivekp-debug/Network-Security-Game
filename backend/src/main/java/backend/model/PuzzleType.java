package backend.model;

/**
 * Supported CPHS puzzle challenge types. The receiver must solve a puzzle of one
 * of these types before a CPHS message can be unlocked. Each type has its own
 * generation and validation logic and its own UI representation.
 */
public enum PuzzleType {
    /**
     * Cryptographic proof-of-work over SHA-256. Receiver searches for a nonce so
     * that {@code SHA-256(challenge + ":" + nonce) == targetHash}. The recovered
     * key is mathematically derived from the nonce, so this type provides
     * cryptographic key gating.
     */
    POW_SHA256,

    /**
     * Time-bound arithmetic challenge. The receiver evaluates a randomly
     * generated expression (e.g. {@code 23 + 45 * 2}) and submits the integer
     * result. The expected answer is verified against {@code targetHash}.
     */
    ARITHMETIC,

    /**
     * Encoded message challenge. The receiver decodes a Base64 (or simple
     * substitution) string and submits the original cleartext, which is verified
     * against {@code targetHash}.
     */
    ENCODED,

    /**
     * Pattern-recognition challenge. The receiver is shown a numeric or symbolic
     * sequence (arithmetic, geometric, Fibonacci-like) and must submit the next
     * value, which is verified against {@code targetHash}.
     */
    PATTERN
}
