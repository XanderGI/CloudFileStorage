package io.github.XanderGI.dto.request;

import io.github.XanderGI.constraint.NonRootDirectoryPath;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for create directory")
public record CreateDirectoryRequestDto(

        @Schema(
                description = "Path when need create directory",
                example = "work/project/"
        )
        @NonRootDirectoryPath
        String path
) {
}