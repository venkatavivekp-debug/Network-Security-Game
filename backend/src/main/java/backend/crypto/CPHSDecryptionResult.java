package backend.crypto;

public class CPHSDecryptionResult {

    private final String plainText;
    private final long puzzleSolveTimeMs;

    public CPHSDecryptionResult(String plainText, long puzzleSolveTimeMs) {
        this.plainText = plainText;
        this.puzzleSolveTimeMs = puzzleSolveTimeMs;
    }

    public String getPlainText() {
        return plainText;
    }

    public long getPuzzleSolveTimeMs() {
        return puzzleSolveTimeMs;
    }
}
