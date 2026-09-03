package io.github.XanderGI.controller.impl;

import io.github.XanderGI.controller.ResourceControllerApi;
import io.github.XanderGI.dto.DownloadResult;
import io.github.XanderGI.dto.ResourceResponseDto;
import io.github.XanderGI.dto.UploadFileItem;
import io.github.XanderGI.dto.request.MoveResourceRequestDto;
import io.github.XanderGI.dto.request.ResourcePathRequestDto;
import io.github.XanderGI.dto.request.SearchRequestDto;
import io.github.XanderGI.dto.request.UploadResourceRequestDto;
import io.github.XanderGI.security.SecurityUser;
import io.github.XanderGI.service.ResourcesService;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

//todo: добавить маппинг: MultipartFile -> UploadFileItem

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
public class ResourceController implements ResourceControllerApi {
    private final ResourcesService resourcesService;

    @GetMapping
    public ResponseEntity<ResourceResponseDto> getInfo(
            @Valid @ModelAttribute ResourcePathRequestDto request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        ResourceResponseDto dto = resourcesService.getResourceInfo(currentUser.getId(), request.path());

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResource(
            @Valid @ModelAttribute ResourcePathRequestDto request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        resourcesService.deleteResource(currentUser.getId(), request.path());

        return ResponseEntity.noContent().build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ResourceResponseDto>> uploadResources(
            @Valid @ModelAttribute UploadResourceRequestDto request,
            @RequestParam("object") List<MultipartFile> files,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        List<UploadFileItem> fileItems = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                fileItems.add(new UploadFileItem(
                        file.getOriginalFilename(),
                        file.getInputStream(),
                        file.getSize()
                ));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        List<ResourceResponseDto> responseList = resourcesService.uploadResources(
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
        List<ResourceResponseDto> responseList = resourcesService.search(currentUser.getId(), request.query());

        return ResponseEntity.ok(responseList);
    }

    @PostMapping("/move")
    public ResponseEntity<ResourceResponseDto> moveResource(
            @Valid @ModelAttribute MoveResourceRequestDto request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        ResourceResponseDto response = resourcesService.moveResource(
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
        DownloadResult content = resourcesService.downloadResource(currentUser.getId(), request.path());

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