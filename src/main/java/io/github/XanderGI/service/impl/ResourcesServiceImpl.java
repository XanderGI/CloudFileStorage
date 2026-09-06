package io.github.XanderGI.service.impl;

import io.github.XanderGI.dto.internal.DownloadResult;
import io.github.XanderGI.dto.response.ResourceResponseDto;
import io.github.XanderGI.dto.response.ResourceType;
import io.github.XanderGI.dto.internal.UploadFileItem;
import io.github.XanderGI.exception.ResourceAlreadyExistsException;
import io.github.XanderGI.exception.ResourceNotFoundException;
import io.github.XanderGI.service.MinioPathHelper;
import io.github.XanderGI.service.ResourcesService;
import io.github.XanderGI.storage.StorageClient;
import io.github.XanderGI.storage.StorageItem;
import io.github.XanderGI.storage.StorageObjectInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//todo: добавить mapper для dto
//todo: подумать стоит ли пытаться добавить фичу для отображения progress-bar при download

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourcesServiceImpl implements ResourcesService {
    private final StorageClient storageClient;
    private final MinioPathHelper helper;

    @Override
    public ResourceResponseDto createDirectory(Long userId, String path) {
        String key = helper.buildMinioKey(userId, path);

        if (storageClient.isExist(key)) {
            throw new ResourceAlreadyExistsException("Failed to create directory: resource already exist.");
        }

        String contextPath = helper.getContextPathFromKey(userId, key);
        String parentKey = helper.buildMinioKey(userId, contextPath);

        if (!contextPath.equals("/") && !storageClient.isExist(parentKey)) {
            throw new ResourceNotFoundException("Failed to create directory: parent folder does not exist");
        }

        storageClient.createFolder(key);

        return toDto(userId, key, null, helper.isFolder(key));
    }

    @Override
    public List<ResourceResponseDto> listDirectory(Long userId, String path) {
        String key = helper.buildMinioKey(userId, path);
        boolean isRoot = path.equals("/");

        if (!isRoot && !storageClient.isExist(key)) {
            throw new ResourceNotFoundException("failed to get list directory: directory does not exist");
        }

        List<StorageItem> storageItems = storageClient.listObjects(key, false);

        return storageItems.stream()
                .map(item ->
                        toDto(userId, item.key(), item.size(), item.isDirectory())
                )
                .toList();
    }

    @Override
    public ResourceResponseDto getResourceInfo(Long userId, String path) {
        String key = helper.buildMinioKey(userId, path);

        if (!storageClient.isExist(key)) {
            throw new ResourceNotFoundException("Failed to get info about resource: resource not found");
        }

        StorageObjectInfo objectInfo = storageClient.statObject(key);
        boolean isDirectory = helper.isFolder(key);

        return toDto(userId, key, objectInfo.size(), isDirectory);
    }

    @Override
    public void deleteResource(Long userId, String path) {
        String rootKey = helper.buildMinioKey(userId, path);

        if (!storageClient.isExist(rootKey)) {
            throw new ResourceNotFoundException("Failed to delete resource: resource not found");
        }

        if (helper.isFolder(rootKey)) {
            List<String> nestedKeys = storageClient.listObjects(rootKey, true).stream()
                    .map(StorageItem::key)
                    .toList();

            storageClient.removeObjects(nestedKeys);
        }

        storageClient.removeObject(rootKey);
    }

    @Override
    public List<ResourceResponseDto> uploadResources(Long userId, String path, List<UploadFileItem> files) {
        List<ResourceResponseDto> responseList = new ArrayList<>();
        Set<String> keysInBatch = new HashSet<>();

        for (UploadFileItem file : files) {
            String fileName = file.originalFilename();
            String filePath = helper.buildFilePath(path, fileName);
            String fileKey = helper.buildMinioKey(userId, filePath);

            if (!keysInBatch.add(fileKey)) {
                throw new ResourceAlreadyExistsException("Failed to upload file: duplicate file \"%s\" in request".formatted(fileName));
            }

            if (storageClient.isExist(fileKey)) {
                throw new ResourceAlreadyExistsException("Failed to upload file: resource \"%s\" already exist".formatted(fileName));
            }
        }

        for (UploadFileItem file : files) {
            String filePath = helper.buildFilePath(path, file.originalFilename());
            String fileKey = helper.buildMinioKey(userId, filePath);
            String fileContextPath = helper.getContextPath(filePath);
            List<String> pathSegments = helper.splitContextPath(fileContextPath);

            for (String segment : pathSegments) {
                String segmentKey = helper.buildMinioKey(userId, segment);

                if (!storageClient.isExist(segmentKey)) {
                    storageClient.createFolder(segmentKey);

                    responseList.add(0, toDto(userId, segmentKey, null, true));
                }
            }

            storageClient.upload(fileKey, file.inputStream(), file.size(), file.originalFilename());

            responseList.add(0, toDto(userId, fileKey, file.size(), false));
        }

        return responseList;
    }

    @Override
    public List<ResourceResponseDto> search(Long userId, String query) {
        String rootKey = helper.buildMinioKey(userId, "");
        String lowerQuery = query.toLowerCase();

        List<StorageItem> items = storageClient.listObjects(rootKey, true);

        return items.stream()
                .filter(item -> {
                    String fileName = helper.getName(item.key()).toLowerCase();
                    return fileName.contains(lowerQuery);
                })
                .map(item -> toDto(userId, item.key(), item.size(), item.isDirectory()))
                .toList();
    }

    @Override
    public DownloadResult downloadResource(Long userId, String path) {
        String key = helper.buildMinioKey(userId, path);

        if (!storageClient.isExist(key)) {
            throw new ResourceNotFoundException("failed to download resource: resource not found");
        }

        String fileName = helper.getName(key);

        if (helper.isFolder(key)) {
            return new DownloadResult(
                    fileName.concat(".zip"),
                    outputStream -> buildZip(key, outputStream)
            );
        } else {
            return new DownloadResult(
                    fileName,
                    outputStream -> {
                        try (InputStream inputStream = storageClient.getObject(key)) {
                            inputStream.transferTo(outputStream);
                        }
                    }
            );
        }
    }

    @Override
    public ResourceResponseDto moveResource(Long userId, String from, String to) {
        String fromKey = helper.buildMinioKey(userId, from);
        String toKey = helper.buildMinioKey(userId, to);
        boolean sourceIsDirectory = helper.isFolder(fromKey);

        if (!storageClient.isExist(fromKey)) {
            throw new ResourceNotFoundException("failed to move resource: resource not found");
        }

        if (sourceIsDirectory != helper.isFolder(toKey)) {
            throw new IllegalArgumentException("Incompatible source and destination types for move operation");
        }

        if (storageClient.isExist(toKey)) {
            throw new ResourceAlreadyExistsException("failed to move resource: resource to target path already exist");
        }

        String contextPathTo = helper.getContextPathFromKey(userId, toKey);
        String contextPathToKey = helper.buildMinioKey(userId, contextPathTo);

        if (!contextPathTo.equals("/") && !storageClient.isExist(contextPathToKey)) {
            throw new ResourceNotFoundException("failed to move resource: context path to target not exist");
        }

        if (sourceIsDirectory) {
            return moveDirectory(userId, fromKey, toKey);
        } else {
            return moveFile(userId, fromKey, toKey);
        }
    }

    private ResourceResponseDto moveDirectory(Long userId, String fromKey, String toKey) {
        List<StorageItem> items = storageClient.listObjects(fromKey, true);

        storageClient.createFolder(toKey);

        for (StorageItem item : items) {
            String fileKey = item.key();
            String suffix = fileKey.substring(fromKey.length());
            String targetKey = toKey.concat(suffix);

            storageClient.copyObject(fileKey, targetKey);
        }

        storageClient.removeObjects(items.stream()
                .map(StorageItem::key)
                .toList()
        );
        storageClient.removeObject(fromKey);

        return toDto(userId, toKey, null, true);
    }

    private ResourceResponseDto moveFile(Long userId, String fromKey, String toKey) {
        Long size = storageClient.statObject(fromKey).size();

        storageClient.copyObject(fromKey, toKey);
        storageClient.removeObject(fromKey);

        return toDto(userId, toKey, size, false);
    }

    private void buildZip(String key, OutputStream outputStream) throws IOException {
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

    private ResourceResponseDto toDto(Long userId, String key, Long size, boolean isDirectory) {
        return new ResourceResponseDto(
                helper.getContextPathFromKey(userId, key),
                helper.getName(key),
                isDirectory ? null : size,
                isDirectory ? ResourceType.DIRECTORY : ResourceType.FILE
        );
    }
}