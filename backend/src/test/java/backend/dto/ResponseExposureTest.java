package backend.dto;

import backend.model.PuzzleType;
import backend.model.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Object-property exposure regression. Serializes user-facing DTOs and asserts
 * that no obviously sensitive field can leak through them. This is a thin
 * outside-threat smoke test, not a substitute for code review.
 */
class ResponseExposureTest {

    private static final List<String> FORBIDDEN = List.of(
            "password",
            "passwordHash",
            "wrappedKey",
            "recoveredKey",
            "solvedNonce",
            "internalSalt",
            "rawFingerprint",
            "rawIp",
            "sessionId",
            "stackTrace"
    );

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void authResponseDoesNotLeakSensitiveFields() throws Exception {
        AuthResponse auth = new AuthResponse("alice", Role.SENDER, "ok");
        assertNoForbiddenFields(mapper.writeValueAsString(auth));
    }

    @Test
    void puzzleChallengeResponseDoesNotLeakSensitiveFields() throws Exception {
        PuzzleChallengeResponse resp = new PuzzleChallengeResponse();
        resp.setMessageId(7L);
        resp.setPuzzleType(PuzzleType.POW_SHA256);
        resp.setQuestion("Find a nonce that hashes to the target.");
        resp.setChallenge("aGVsbG8td29ybGQ=");
        resp.setTargetHash("00ab...");
        resp.setMaxIterations(180_000);
        resp.setAttemptsAllowed(3);
        resp.setAttemptsUsed(0);
        resp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        resp.setSolved(false);

        String json = mapper.writeValueAsString(resp);
        assertNoForbiddenFields(json);
        // The puzzle response intentionally exposes targetHash for PoW so the
        // client can mine. It must not expose the recovered key or solved nonce.
        assertFalse(json.contains("recoveredKey"));
        assertFalse(json.contains("solvedNonce"));
    }

    @Test
    void apiErrorResponseDoesNotLeakStackTraceOrException() throws Exception {
        ApiErrorResponse err = new ApiErrorResponse();
        err.setTimestamp(LocalDateTime.now());
        err.setStatus(403);
        err.setError("Forbidden");
        err.setMessage("Access denied");
        err.setPath("/admin/lock-user");
        err.setDetails(List.of("You do not have permission to perform this action"));

        String json = mapper.writeValueAsString(err);
        assertNoForbiddenFields(json);
        assertFalse(json.contains("Exception"));
        assertFalse(json.contains("at backend."));
    }

    private void assertNoForbiddenFields(String json) {
        for (String field : FORBIDDEN) {
            assertTrue(!json.toLowerCase().contains(field.toLowerCase()),
                    () -> "Forbidden field '" + field + "' leaked into response: " + json);
        }
    }
}
