package io.github.XanderGI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CloudFileStorageApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudFileStorageApplication.class, args);
    }
}