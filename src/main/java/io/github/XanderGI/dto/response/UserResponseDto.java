package io.github.XanderGI.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after registration or authenticated with username")
public record UserResponseDto(
        @Schema(
                description = "Username of the currently authorized user",
                example = "xander"
        )
        String username
) {
}