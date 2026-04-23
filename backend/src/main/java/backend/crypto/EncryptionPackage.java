package backend.crypto;

public class EncryptionPackage {

    private final String encryptedContent;
    private final String metadata;

    public EncryptionPackage(String encryptedContent, String metadata) {
        this.encryptedContent = encryptedContent;
        this.metadata = metadata;
    }

    public String getEncryptedContent() {
        return encryptedContent;
    }

    public String getMetadata() {
        return metadata;
    }
}
