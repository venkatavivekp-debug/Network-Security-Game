package backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PuzzleSolveRequest {

    @NotNull(message = "nonce is required")
    @Min(value = 0, message = "nonce must be non-negative")
    private Integer nonce;

    public Integer getNonce() {
        return nonce;
    }

    public void setNonce(Integer nonce) {
        this.nonce = nonce;
    }
}

