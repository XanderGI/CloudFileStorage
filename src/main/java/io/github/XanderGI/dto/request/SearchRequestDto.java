package io.github.XanderGI.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request containing a search query")
public record SearchRequestDto(

        @Schema(
                description = "Query to find resources",
                example = "docs"
        )
        @NotBlank(message = "query must not be missing and blank")
        String query
) {
}