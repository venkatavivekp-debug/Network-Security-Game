package backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cphs")
public class CphsProperties {

    private int randomKeyBytes;

    public int getRandomKeyBytes() {
        return randomKeyBytes;
    }

    public void setRandomKeyBytes(int randomKeyBytes) {
        this.randomKeyBytes = randomKeyBytes;
    }
}
