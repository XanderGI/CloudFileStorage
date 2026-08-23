package io.github.XanderGI.service.impl;

import io.github.XanderGI.dto.ResourceResponseDto;
import io.github.XanderGI.dto.ResourceType;
import io.github.XanderGI.dto.UploadFileItem;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//todo: добавить mapper для dto

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
    public List<ResourceResponseDto> uploadFiles(Long userId, String path, List<UploadFileItem> files) {
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

    private ResourceResponseDto toDto(Long userId, String key, Long size, boolean isDirectory) {
        return new ResourceResponseDto(
                helper.getContextPathFromKey(userId, key),
                helper.getName(key),
                isDirectory ? null : size,
                isDirectory ? ResourceType.DIRECTORY : ResourceType.FILE
        );
    }
}