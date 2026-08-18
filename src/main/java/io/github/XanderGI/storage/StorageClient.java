package io.github.XanderGI.storage;

import java.io.InputStream;
import java.util.List;

public interface StorageClient {
    void createFolder(String key);

    StorageItem upload(String key, InputStream inputStream);

    InputStream getObject(String key);

    StorageObjectInfo statObject(String key);

    boolean exist(String key);

    List<StorageItem> listObjects(String key, boolean isRecursive);

    void removeObject(String key);

    void copyObject(String fromKey, String toKey);
}