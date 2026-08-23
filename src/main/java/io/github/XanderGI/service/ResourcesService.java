package io.github.XanderGI.service;

import io.github.XanderGI.dto.ResourceResponseDto;
import io.github.XanderGI.dto.UploadFileItem;

import java.util.List;

public interface ResourcesService {
    ResourceResponseDto createDirectory(Long userId, String path);

    List<ResourceResponseDto> listDirectory(Long userId, String path);

    ResourceResponseDto getResourceInfo(Long userId, String path);

    void deleteResource(Long userId, String path);

    List<ResourceResponseDto> uploadFiles(Long userId, String path, List<UploadFileItem> files);
}