package io.github.XanderGI.controller;

import io.github.XanderGI.dto.ResourceResponseDto;
import io.github.XanderGI.security.SecurityUser;
import io.github.XanderGI.service.ResourcesService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
public class DirectoryController {
    private final ResourcesService resourcesService;

    @PostMapping
    public ResponseEntity<ResourceResponseDto> createFolder(
            @Pattern(regexp = "^.+[^/].*/$", message = "Folder path must be non-empty and end with /") @RequestParam String path,
            @AuthenticationPrincipal SecurityUser currentUser
            ) {
        ResourceResponseDto dto = resourcesService.createDirectory(currentUser.getId(), path);

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}