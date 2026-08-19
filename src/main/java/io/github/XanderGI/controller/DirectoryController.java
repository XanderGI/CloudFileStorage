package io.github.XanderGI.controller;

import io.github.XanderGI.dto.ResourceResponseDto;
import io.github.XanderGI.security.SecurityUser;
import io.github.XanderGI.service.ResourcesService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
public class DirectoryController {
    private final ResourcesService resourcesService;

    @PostMapping
    public ResponseEntity<ResourceResponseDto> createFolder(
            @Pattern(regexp = "^.*[^/].*/$", message = "Directory path must be non-empty and end with /") @RequestParam String path,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        ResourceResponseDto dto = resourcesService.createDirectory(currentUser.getId(), path);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(dto);
    }

    @GetMapping
    public ResponseEntity<List<ResourceResponseDto>> getFolderInfo(
            @Pattern(regexp = "^(/|.*[^/].*/)$", message = "Directory path must be non-empty and end with /") @RequestParam String path,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        List<ResourceResponseDto> list = resourcesService.listDirectory(currentUser.getId(), path);

        return ResponseEntity.ok(list);
    }
}