package io.github.XanderGI.controller;

import io.github.XanderGI.dto.ErrorResponseDto;
import io.github.XanderGI.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

@Tag(name = "Users", description = "Authenticated user information")
@SecurityRequirement(name = "cookieAuth")
public interface UserControllerApi {

    @Operation(summary = "Get current user", description = "Returns current authenticated username")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Information about the current user has been successfully received",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "User is not authorized",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            ),
    })
    ResponseEntity<UserResponseDto> getCurrentUser(Principal principal);
}