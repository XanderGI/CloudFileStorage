package io.github.XanderGI.service;

import io.github.XanderGI.storage.StorageClient;
import io.github.XanderGI.storage.StorageItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
public class ResourceZipBuilder {
    private final StorageClient storageClient;

    public void buildZip(String key, OutputStream outputStream) throws IOException {
        List<StorageItem> items = storageClient.listObjects(key, true);

        try (ZipOutputStream zipStream = new ZipOutputStream(outputStream)) {
            for (StorageItem item : items) {
                String itemKey = item.key();

                if (item.isDirectory()) {
                    continue;
                }

                String entryName = itemKey.substring(key.length());
                zipStream.putNextEntry(new ZipEntry(entryName));

                try (InputStream fileStream = storageClient.getObject(itemKey)) {
                    fileStream.transferTo(zipStream);
                }

                zipStream.closeEntry();
            }
        }
    }
}