package io.github.XanderGI.controller;

import io.github.XanderGI.dto.response.ErrorResponseDto;
import io.github.XanderGI.dto.response.ResourceResponseDto;
import io.github.XanderGI.dto.request.CreateDirectoryRequestDto;
import io.github.XanderGI.dto.request.DirectoryPathRequestDto;
import io.github.XanderGI.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Directory", description = "Directory create and get contents")
@ResourceEndpointResponses
public interface DirectoryControllerApi {

    @Operation(summary = "Create new directory")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Directory created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Directory is not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Directory is already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @SecurityRequirement(name = "cookieAuth")
    ResponseEntity<ResourceResponseDto> createDirectory(
            @Valid @ParameterObject CreateDirectoryRequestDto request,
            @Parameter(hidden = true)
            SecurityUser currentUser
    );

    @Operation(summary = "Get information about directory")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful receipt of directory information",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(implementation = ResourceResponseDto.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Directory is not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @SecurityRequirement(name = "cookieAuth")
    ResponseEntity<List<ResourceResponseDto>> getDirectoryInfo(
            @Valid @ParameterObject DirectoryPathRequestDto request,
            @Parameter(hidden = true)
            SecurityUser currentUser
    );
}