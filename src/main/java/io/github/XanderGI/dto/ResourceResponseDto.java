package io.github.XanderGI.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information about resource(file or directory)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceResponseDto(

        @Schema(
                description = "Contextual path to the file relative to the user's root storage",
                example = "work/project/"
        )
        String path,

        @Schema(
                description = "Resource name",
                example = "cloud file storage.war"
        )
        String name,

        @Schema(
                description = "Size of the resource in bytes, there isn't size for directories",
                example = "101408135",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Long size,

        @Schema(
                description = "Resource type",
                example = "FILE"
        )
        ResourceType type
) {
}