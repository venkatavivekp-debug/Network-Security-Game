package backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Receiver-supplied solve attempt. For {@link backend.model.PuzzleType#POW_SHA256}
 * the {@link #nonce} field is required. For all other puzzle types the
 * {@link #answer} field is required. The puzzle engine that owns the puzzle
 * row decides which field to consume.
 */
public class PuzzleSolveRequest {

    @Min(value = 0, message = "nonce must be non-negative")
    private Integer nonce;

    @Size(max = 256, message = "answer must be at most 256 characters")
    private String answer;

    public Integer getNonce() {
        return nonce;
    }

    public void setNonce(Integer nonce) {
        this.nonce = nonce;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
