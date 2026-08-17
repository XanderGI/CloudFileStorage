package io.github.XanderGI.storage.impl;

import io.github.XanderGI.dto.ResourceResponseDto;
import io.github.XanderGI.exception.MinioStorageException;
import io.github.XanderGI.storage.StorageClient;
import io.github.XanderGI.storage.StorageObjectInfo;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MinioStorageClient implements StorageClient {
    private final MinioClient client;

    @Value("${minio.bucket}")
    private String bucketName;

    @Override
    public void createFolder(String key) {
        try (InputStream inputStream = new ByteArrayInputStream(new byte[]{})) {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .stream(inputStream, 0L, -1L)
                            .build()
            );
        } catch (MinioException | IOException e) {
            throw new MinioStorageException("Failed to create folder", e);
        }
    }

    @Override
    public ResourceResponseDto upload(String key, InputStream inputStream) {
        return null;
    }

    @Override
    public InputStream getObject(String key) {
        return null;
    }

    @Override
    public StorageObjectInfo statObject(String key) {
        try {
            StatObjectResponse statResp = client.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build()
            );

            return new StorageObjectInfo(statResp.size());
        } catch (MinioException e) {
            throw new MinioStorageException("Failed to get info about resource", e);
        }
    }

    @Override
    public boolean exist(String key) {
        try {
            client.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build()
            );

            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }

            throw new MinioStorageException("Failed to check exist resource", e);
        } catch (MinioException e) {
            throw new MinioStorageException("Failed to check exist resource", e);
        }
    }

    @Override
    public List<ResourceResponseDto> listObjects(String key, boolean recursive) {
        return List.of();
    }

    @Override
    public void removeObject(String key) {

    }

    @Override
    public void copyObject(String fromKey, String toKey) {

    }
}