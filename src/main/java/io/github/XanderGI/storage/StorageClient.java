package io.github.XanderGI.storage;

import io.github.XanderGI.dto.ResourceResponseDto;

import java.io.InputStream;
import java.util.List;

public interface StorageClient {
    void createFolder(String key);

    ResourceResponseDto upload(String key, InputStream inputStream);

    InputStream getObject(String key);

    StorageObjectInfo statObject(String key);

    boolean exist(String key);

    List<ResourceResponseDto> listObjects(String key, boolean recursive);

    void removeObject(String key);

    void copyObject(String fromKey, String toKey);
}