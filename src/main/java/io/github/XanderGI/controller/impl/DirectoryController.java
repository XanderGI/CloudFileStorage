package io.github.XanderGI.controller.impl;

import io.github.XanderGI.controller.DirectoryControllerApi;
import io.github.XanderGI.dto.response.ResourceResponseDto;
import io.github.XanderGI.dto.request.CreateDirectoryRequestDto;
import io.github.XanderGI.dto.request.DirectoryPathRequestDto;
import io.github.XanderGI.security.SecurityUser;
import io.github.XanderGI.service.ResourcesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
public class DirectoryController implements DirectoryControllerApi {
    private final ResourcesService resourcesService;

    @PostMapping
    public ResponseEntity<ResourceResponseDto> createDirectory(
            @Valid @ModelAttribute CreateDirectoryRequestDto request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        ResourceResponseDto dto = resourcesService.createDirectory(currentUser.getId(), request.path());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(dto);
    }

    @GetMapping
    public ResponseEntity<List<ResourceResponseDto>> getDirectoryInfo(
            @Valid @ModelAttribute DirectoryPathRequestDto request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        List<ResourceResponseDto> list = resourcesService.listDirectory(currentUser.getId(), request.path());

        return ResponseEntity.ok(list);
    }
}