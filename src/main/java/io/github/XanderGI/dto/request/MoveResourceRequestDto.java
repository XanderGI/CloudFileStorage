package io.github.XanderGI.dto.request;

import io.github.XanderGI.constraint.NonRootResourcePath;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for moving or renaming resource")
public record MoveResourceRequestDto(

        @Schema(
                description = "Source path",
                example = "doc/source_directory/"
        )
        @NonRootResourcePath(message = "Parameter `from` must not be missing or must be a valid resource path")
        String from,

        @Schema(
                description = "Target path",
                example = "doc/target_directory/"
        )
        @NonRootResourcePath(message = "Parameter `to` must not be missing or must be a valid resource path")
        String to
) {
}