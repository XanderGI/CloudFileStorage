package io.github.XanderGI.dto.request;

import io.github.XanderGI.constraint.UploadTargetPath;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for uploading resources")
public record UploadResourceRequestDto(

        @Schema(
                description = "Path to the target directory",
                example = "work/project/"
        )
        @UploadTargetPath
        String path
) {
    public UploadResourceRequestDto {
        if (path != null && path.isBlank()) {
            path = "/";
        }
    }
}