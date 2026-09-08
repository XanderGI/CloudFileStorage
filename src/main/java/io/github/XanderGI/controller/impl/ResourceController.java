package io.github.XanderGI.controller.impl;

import io.github.XanderGI.controller.ResourceControllerApi;
import io.github.XanderGI.dto.internal.DownloadResult;
import io.github.XanderGI.dto.internal.UploadFileItem;
import io.github.XanderGI.dto.request.MoveResourceRequestDto;
import io.github.XanderGI.dto.request.ResourcePathRequestDto;
import io.github.XanderGI.dto.request.SearchRequestDto;
import io.github.XanderGI.dto.request.UploadResourceRequestDto;
import io.github.XanderGI.dto.response.ResourceResponseDto;
import io.github.XanderGI.mapper.UploadFileItemMapper;
import io.github.XanderGI.security.SecurityUser;
import io.github.XanderGI.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
public class ResourceController implements ResourceControllerApi {
    private final ResourceService resourceService;
    private final UploadFileItemMapper mapper;

    @GetMapping
    public ResponseEntity<ResourceResponseDto> getInfo(
            @Valid @ModelAttribute ResourcePathRequestDto request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        ResourceResponseDto dto = resourceService.getResourceInfo(currentUser.getId(), request.path());

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResource(
            @Valid @ModelAttribute ResourcePathRequestDto request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        resourceService.deleteResource(currentUser.getId(), request.path());

        return ResponseEntity.noContent().build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ResourceResponseDto>> uploadResources(
            @Valid @ModelAttribute UploadResourceRequestDto request,
            @RequestParam("object") List<MultipartFile> files,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        List<UploadFileItem> fileItems = mapper.toUploadFileItems(files);

        List<ResourceResponseDto> responseList = resourceService.uploadResources(
                currentUser.getId(),
                request.path(),
                fileItems
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responseList);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceResponseDto>> search(
            @Valid @ModelAttribute SearchRequestDto request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        List<ResourceResponseDto> responseList = resourceService.search(currentUser.getId(), request.query());

        return ResponseEntity.ok(responseList);
    }

    @PostMapping("/move")
    public ResponseEntity<ResourceResponseDto> moveResource(
            @Valid @ModelAttribute MoveResourceRequestDto request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        ResourceResponseDto response = resourceService.moveResource(
                currentUser.getId(),
                request.from(),
                request.to()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadResource(
            @Valid @ModelAttribute ResourcePathRequestDto request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        DownloadResult content = resourceService.downloadResource(currentUser.getId(), request.path());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .headers(httpHeaders -> httpHeaders.setContentDisposition(
                        ContentDisposition.attachment()
                                .filename(content.filename(), StandardCharsets.UTF_8)
                                .build()
                ))
                .body(content.resourceStream()::writeTo);
    }
}