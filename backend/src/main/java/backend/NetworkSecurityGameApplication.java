package backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "backend.config")
public class NetworkSecurityGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetworkSecurityGameApplication.class, args);
    }
}
