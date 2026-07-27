package io.github.XanderGI.config.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer implements ApplicationRunner {
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean isBucketExist = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );

        if (!isBucketExist) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info("MinIO bucket with name {} successfully created.", bucketName);
        } else {
            log.info("MinIO bucket with name {} already exists. Skipping initialization.", bucketName);
        }
    }
}