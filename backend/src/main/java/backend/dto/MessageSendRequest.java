package backend.dto;

import backend.model.AlgorithmType;
import backend.model.PuzzleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MessageSendRequest {

    @NotBlank(message = "receiverUsername is required")
    @Size(min = 3, max = 40, message = "receiverUsername must be between 3 and 40 characters")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "receiverUsername may contain only letters, numbers, dot, underscore, or hyphen")
    private String receiverUsername;

    @NotBlank(message = "content is required")
    @Size(min = 1, max = 10000, message = "content must be between 1 and 10000 characters")
    private String content;

    @NotNull(message = "algorithmType is required")
    private AlgorithmType algorithmType;

    /**
     * Optional. When {@link #algorithmType} is CPHS the sender can pick a puzzle
     * challenge type. Defaults to {@link PuzzleType#POW_SHA256} for backward
     * compatibility.
     */
    private PuzzleType puzzleType;

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    public PuzzleType getPuzzleType() {
        return puzzleType;
    }

    public void setPuzzleType(PuzzleType puzzleType) {
        this.puzzleType = puzzleType;
    }
}
