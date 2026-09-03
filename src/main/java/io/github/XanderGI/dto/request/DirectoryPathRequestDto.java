package io.github.XanderGI.dto.request;

import io.github.XanderGI.constraint.AnyDirectoryPath;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for get info about directory")
public record DirectoryPathRequestDto(

        @Schema(
                description = "Directory path to inspect (empty for root)",
                example = "work/project/"
        )
        @AnyDirectoryPath
        String path
) {
    public DirectoryPathRequestDto {
        if (path != null && path.isBlank()) {
            path = "/";
        }
    }
}