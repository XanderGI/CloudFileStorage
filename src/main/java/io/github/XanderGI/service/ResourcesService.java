package io.github.XanderGI.service;

import io.github.XanderGI.dto.ResourceResponseDto;

import java.util.List;

public interface ResourcesService {
    ResourceResponseDto createDirectory(Long userId, String path);

    List<ResourceResponseDto> listDirectory(Long userId, String path);
}