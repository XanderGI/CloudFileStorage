package io.github.XanderGI.storage;

import java.io.InputStream;
import java.util.List;

public interface StorageClient {
    void createFolder(String key);

    void upload(String key, InputStream inputStream, long size, String filename);

    InputStream getObject(String key);

    StorageObjectInfo statObject(String key);

    boolean isExist(String key);

    List<StorageItem> listObjects(String key, boolean isRecursive);

    void removeObject(String key);

    void removeObjects(List<String> keys);

    void copyObject(String fromKey, String toKey);
}