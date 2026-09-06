package io.github.XanderGI.config.minio;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioClientConfig {

    @Bean
    public MinioClient minioClient(MinioProperties minioProperties) {
        return MinioClient.builder()
                .endpoint(minioProperties
                        .endpoint()
                        .url())
                .credentials(minioProperties.accessKey(), minioProperties.secretKey())
                .build();
    }
}