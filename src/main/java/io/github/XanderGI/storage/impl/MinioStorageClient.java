package io.github.XanderGI.storage.impl;

import io.github.XanderGI.config.minio.MinioProperties;
import io.github.XanderGI.exception.MinioStorageException;
import io.github.XanderGI.mapper.StorageItemMapper;
import io.github.XanderGI.storage.StorageClient;
import io.github.XanderGI.storage.StorageItem;
import io.github.XanderGI.storage.StorageObjectInfo;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class MinioStorageClient implements StorageClient {
    private final MinioClient client;
    private final MinioProperties minioProperties;
    private final StorageItemMapper mapper;

    @Override
    public void createFolder(String key) {
        try (InputStream inputStream = new ByteArrayInputStream(new byte[]{})) {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(key)
                            .stream(inputStream, 0L, -1L)
                            .build()
            );
        } catch (MinioException | IOException e) {
            throw new MinioStorageException("Failed to create folder", e);
        }
    }

    @Override
    public void upload(String key, InputStream inputStream, long size, String filename) {
        String contentType = MediaTypeFactory.getMediaType(filename)
                .map(MimeType::toString)
                .orElse("application/octet-stream");
        try {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(key)
                            .stream(inputStream, size, -1L)
                            .contentType(contentType)
                            .build()
            );

        } catch (MinioException e) {
            throw new MinioStorageException("Failed to upload resource", e);
        }
    }

    @Override
    public InputStream getObject(String key) {
        try {
            return client.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(key)
                            .build()
            );
        } catch (MinioException e) {
            throw new MinioStorageException("Failed to download resource", e);
        }
    }

    @Override
    public StorageObjectInfo statObject(String key) {
        try {
            StatObjectResponse statResp = client.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(key)
                            .build()
            );

            return new StorageObjectInfo(statResp.size());
        } catch (MinioException e) {
            throw new MinioStorageException("Failed to get info about resource", e);
        }
    }

    @Override
    public boolean isExist(String key) {
        try {
            client.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.bucket())
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
    public List<StorageItem> listObjects(String key, boolean isRecursive) {
        Iterable<Result<Item>> results = fetchRawObjects(key, isRecursive);

        return StreamSupport.stream(results.spliterator(), false)
                .map(this::unwrapResult)
                .filter(item -> !item.objectName().equals(key))
                .map(mapper::toStorageItem)
                .toList();
    }

    @Override
    public void removeObject(String key) {
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(key)
                            .build()
            );
        } catch (MinioException e) {
            throw new MinioStorageException("Failed to delete resource", e);
        }
    }

    @Override
    public void removeObjects(List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }

        Iterable<Result<DeleteResult.Error>> results = client.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(minioProperties.bucket())
                        .objects(keys.stream()
                                .map(DeleteRequest.Object::new)
                                .toList()
                        )
                        .build()
        );

        StreamSupport.stream(results.spliterator(), false)
                .forEach(this::unwrapResult);
    }

    @Override
    public void copyObject(String fromKey, String toKey) {
        try {
            client.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(toKey)
                            .source(SourceObject.builder()
                                    .bucket(minioProperties.bucket())
                                    .object(fromKey)
                                    .build())
                            .build()
            );
        } catch (MinioException e) {
            throw new MinioStorageException("Failed to copy resource", e);
        }
    }

    private Iterable<Result<Item>> fetchRawObjects(String key, boolean isRecursive) {
        return client.listObjects(
                ListObjectsArgs.builder()
                        .bucket(minioProperties.bucket())
                        .prefix(key)
                        .recursive(isRecursive)
                        .build()
        );
    }

    private <T> T unwrapResult(Result<T> result) {
        try {
            return result.get();
        } catch (MinioException e) {
            throw new MinioStorageException("Failed to process storage result", e);
        }
    }
}