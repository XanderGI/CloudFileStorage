package io.github.XanderGI.controller;

import io.github.XanderGI.dto.ResourceResponseDto;
import io.github.XanderGI.dto.UploadFileItem;
import io.github.XanderGI.security.SecurityUser;
import io.github.XanderGI.service.ResourcesService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//todo: добавить маппинг: MultipartFile -> UploadFileItem

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
public class ResourceController {
    private final ResourcesService resourcesService;

    @GetMapping
    public ResponseEntity<ResourceResponseDto> getInfo(
            @Pattern(regexp = "^(/|.*[^/].*/?)$", message = "Resource path must be non-empty") @RequestParam String path,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        ResourceResponseDto dto = resourcesService.getResourceInfo(currentUser.getId(), path);

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResource(
            @Pattern(regexp = "^(/|.*[^/].*/?)$", message = "Resource path must be non-empty") @RequestParam String path,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        resourcesService.deleteResource(currentUser.getId(), path);

        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<List<ResourceResponseDto>> uploadFiles(
            @Pattern(regexp = "^(/|.*[^/].*/)$", message = "Resource path must be non-empty and end with /") @RequestParam String path,
            @NotEmpty(message = "Files list cannot be empty") @RequestParam List<MultipartFile> files,
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

        List<ResourceResponseDto> responseList = resourcesService.uploadFiles(currentUser.getId(), path, fileItems);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responseList);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceResponseDto>> search(
            @NotBlank(message = "query must not be blank") @RequestParam String query,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        List<ResourceResponseDto> responseList = resourcesService.search(currentUser.getId(), query);

        return ResponseEntity.ok(responseList);
    }

}