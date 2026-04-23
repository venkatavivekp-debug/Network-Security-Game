package backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.crypto")
public class CryptoProperties {

    private String masterKey;
    private String shcsKey;
    private int ivLengthBytes;
    private String aesTransformation;
    private int gcmTagLengthBits;

    public String getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }

    public String getShcsKey() {
        return shcsKey;
    }

    public void setShcsKey(String shcsKey) {
        this.shcsKey = shcsKey;
    }

    public int getIvLengthBytes() {
        return ivLengthBytes;
    }

    public void setIvLengthBytes(int ivLengthBytes) {
        this.ivLengthBytes = ivLengthBytes;
    }

    public String getAesTransformation() {
        return aesTransformation;
    }

    public void setAesTransformation(String aesTransformation) {
        this.aesTransformation = aesTransformation;
    }

    public int getGcmTagLengthBits() {
        return gcmTagLengthBits;
    }

    public void setGcmTagLengthBits(int gcmTagLengthBits) {
        this.gcmTagLengthBits = gcmTagLengthBits;
    }
}
