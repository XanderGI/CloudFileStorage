package io.github.XanderGI.mapper;

import io.github.XanderGI.dto.response.ResourceResponseDto;
import io.github.XanderGI.dto.response.ResourceType;
import org.springframework.stereotype.Component;

@Component
public class ResourceDtoMapper {

    public ResourceResponseDto toResourceResponseDto(String path, String name, Long size, boolean isDirectory) {
        return new ResourceResponseDto(
                path,
                name,
                isDirectory ? null : size,
                isDirectory ? ResourceType.DIRECTORY : ResourceType.FILE
        );
    }
}