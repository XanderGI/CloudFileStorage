package io.github.XanderGI.dto.request;

import io.github.XanderGI.constraint.NonRootResourcePath;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for moving or renaming resource")
public record MoveResourceRequestDto(

        @Schema(
                description = "Source path",
                example = "doc/source_directory/"
        )
        @NonRootResourcePath
        String from,

        @Schema(
                description = "Target path",
                example = "doc/target_directory/"
        )
        @NonRootResourcePath
        String to
) {
}