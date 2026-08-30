package io.github.XanderGI.dto.request;

import io.github.XanderGI.constraint.AnyResourcePath;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request containing a path to an existing resource")
public record ResourcePathRequestDto(

        @Schema(
                description = "Path to resource",
                example = "folder1/folder2/file.txt"
        )
        @AnyResourcePath
        String path
) {
}