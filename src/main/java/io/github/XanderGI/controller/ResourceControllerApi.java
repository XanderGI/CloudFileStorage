package io.github.XanderGI.controller;

import io.github.XanderGI.dto.response.ErrorResponseDto;
import io.github.XanderGI.dto.response.ResourceResponseDto;
import io.github.XanderGI.dto.request.MoveResourceRequestDto;
import io.github.XanderGI.dto.request.ResourcePathRequestDto;
import io.github.XanderGI.dto.request.SearchRequestDto;
import io.github.XanderGI.dto.request.UploadResourceRequestDto;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@Tag(name = "Resource", description = "File and folder resource management")
@ResourceEndpointResponses
public interface ResourceControllerApi {

    @Operation(summary = "Get resource information")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieve resource information",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource is not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @SecurityRequirement(name = "cookieAuth")
    ResponseEntity<ResourceResponseDto> getInfo(
            @Valid @ParameterObject ResourcePathRequestDto request,
            @Parameter(hidden = true)
            SecurityUser currentUser
    );

    @Operation(summary = "Delete resource")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Successfully deleted resource"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource is not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @SecurityRequirement(name = "cookieAuth")
    ResponseEntity<Void> deleteResource(
            @Valid @ParameterObject ResourcePathRequestDto request,
            @Parameter(hidden = true)
            SecurityUser currentUser
    );

    @Operation(summary = "Upload resources")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Uploaded resources successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(implementation = ResourceResponseDto.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Resource is already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @SecurityRequirement(name = "cookieAuth")
    ResponseEntity<List<ResourceResponseDto>> uploadResources(
            @Valid @ParameterObject UploadResourceRequestDto request,
            @Parameter(
                    name = "object",
                    description = "Files to upload",
                    required = true
            )
            List<MultipartFile> files,
            @Parameter(hidden = true)
            SecurityUser currentUser);

    @Operation(summary = "Search resource by query")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved search results",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(implementation = ResourceResponseDto.class)
                            )
                    )
            )
    })
    @SecurityRequirement(name = "cookieAuth")
    ResponseEntity<List<ResourceResponseDto>> search(
            @Valid @ParameterObject SearchRequestDto request,
            @Parameter(hidden = true)
            SecurityUser currentUser
    );

    @Operation(summary = "Move or rename resources")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully move or rename resource",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource is not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Resource already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @SecurityRequirement(name = "cookieAuth")
    ResponseEntity<ResourceResponseDto> moveResource(
            @Valid @ParameterObject MoveResourceRequestDto request,
            @Parameter(hidden = true)
            SecurityUser currentUser
    );

    @Operation(summary = "Download resources")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully downloaded resources",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource is not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @SecurityRequirement(name = "cookieAuth")
    ResponseEntity<StreamingResponseBody> downloadResource(
            @Valid @ParameterObject ResourcePathRequestDto request,
            @Parameter(hidden = true)
            SecurityUser currentUser
    );
}