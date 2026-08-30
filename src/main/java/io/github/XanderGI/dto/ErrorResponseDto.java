package io.github.XanderGI.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response")
public record ErrorResponseDto(
        @Schema(
                description = "Client error message in the context of the problem",
                example = "error message"
        )
        String message
) {
}