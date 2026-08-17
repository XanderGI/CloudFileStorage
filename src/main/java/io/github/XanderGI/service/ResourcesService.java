package io.github.XanderGI.service;

import io.github.XanderGI.dto.ResourceResponseDto;

public interface ResourcesService {
    ResourceResponseDto createDirectory(Long userId, String path);
}