package io.github.XanderGI.service.impl;

import io.github.XanderGI.dto.internal.DownloadResult;
import io.github.XanderGI.dto.internal.UploadFileItem;
import io.github.XanderGI.dto.response.ResourceResponseDto;
import io.github.XanderGI.exception.ResourceAlreadyExistsException;
import io.github.XanderGI.exception.ResourceNotFoundException;
import io.github.XanderGI.mapper.ResourceDtoMapper;
import io.github.XanderGI.service.MinioPathHelper;
import io.github.XanderGI.service.ResourceService;
import io.github.XanderGI.service.ResourceZipBuilder;
import io.github.XanderGI.storage.StorageClient;
import io.github.XanderGI.storage.StorageItem;
import io.github.XanderGI.storage.StorageObjectInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    private final StorageClient storageClient;
    private final ResourceZipBuilder zipBuilder;
    private final MinioPathHelper helper;
    private final ResourceDtoMapper mapper;

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

        log.info("Directory {} created, for user {}", helper.getName(key), userId);

        return buildResourceResponse(userId, key, null, helper.isFolder(key));
    }

    @Override
    public List<ResourceResponseDto> getDirectoryContent(Long userId, String path) {
        String key = helper.buildMinioKey(userId, path);
        boolean isRoot = path.equals("/");

        if (!isRoot && !storageClient.isExist(key)) {
            throw new ResourceNotFoundException("failed to get list directory: directory does not exist");
        }

        List<StorageItem> storageItems = storageClient.listObjects(key, false);

        return storageItems.stream()
                .map(item ->
                        buildResourceResponse(userId, item.key(), item.size(), item.isDirectory())
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

        return buildResourceResponse(userId, key, objectInfo.size(), isDirectory);
    }

    @Override
    public void deleteResource(Long userId, String path) {
        String rootKey = helper.buildMinioKey(userId, path);
        List<String> nestedKeys = new ArrayList<>();

        if (!storageClient.isExist(rootKey)) {
            throw new ResourceNotFoundException("Failed to delete resource: resource not found");
        }

        if (helper.isFolder(rootKey)) {
            nestedKeys = storageClient.listObjects(rootKey, true).stream()
                    .map(StorageItem::key)
                    .toList();

            storageClient.removeObjects(nestedKeys);
        }

        storageClient.removeObject(rootKey);

        int totalDeletedResources = nestedKeys.size() + 1;
        log.info("Success deleted {} resources, for user {}", totalDeletedResources, userId);
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

                    responseList.add(0, buildResourceResponse(userId, segmentKey, null, true));
                }
            }

            storageClient.upload(fileKey, file.inputStream(), file.size(), file.originalFilename());

            responseList.add(0, buildResourceResponse(userId, fileKey, file.size(), false));
        }

        log.info("Uploaded {} resources for user {}", responseList.size(), userId);

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
                .map(item -> buildResourceResponse(userId, item.key(), item.size(), item.isDirectory()))
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
                    outputStream -> zipBuilder.buildZip(key, outputStream)
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

        int totalMovedResources = items.size() + 1;
        log.info("Resources {} moved: from {}, to {}, for user {}", totalMovedResources, fromKey, toKey, userId);

        return buildResourceResponse(userId, toKey, null, true);
    }

    private ResourceResponseDto moveFile(Long userId, String fromKey, String toKey) {
        Long size = storageClient.statObject(fromKey).size();

        storageClient.copyObject(fromKey, toKey);
        storageClient.removeObject(fromKey);

        log.info("Resource moved: from {}, to {}, for user {}", fromKey, toKey, userId);

        return buildResourceResponse(userId, toKey, size, false);
    }

    private ResourceResponseDto buildResourceResponse(Long userId, String key, Long size, boolean isDirectory) {
        String path = helper.getContextPathFromKey(userId, key);
        String name = helper.getName(key);

        return mapper.toResourceResponseDto(path, name, size, isDirectory);
    }
}