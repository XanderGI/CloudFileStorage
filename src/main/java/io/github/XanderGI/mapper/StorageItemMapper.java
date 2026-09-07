package io.github.XanderGI.mapper;

import io.github.XanderGI.storage.StorageItem;
import io.minio.messages.Item;
import org.springframework.stereotype.Component;

@Component
public class StorageItemMapper {

    public StorageItem toStorageItem(Item item) {
        boolean isDirectory = item.objectName().endsWith("/");

        return new StorageItem(
                item.objectName(),
                isDirectory ? null : item.size(),
                isDirectory
        );
    }
}