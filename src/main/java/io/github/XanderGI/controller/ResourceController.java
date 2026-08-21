package io.github.XanderGI.controller;

import io.github.XanderGI.dto.ResourceResponseDto;
import io.github.XanderGI.security.SecurityUser;
import io.github.XanderGI.service.ResourcesService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
public class ResourceController {
    private final ResourcesService resourcesService;

    @GetMapping
    public ResponseEntity<ResourceResponseDto> getInfo(
            @Pattern(regexp = "^(/|.*[^/].*/?)$", message = "Resource path must be non-empty and end with /") @RequestParam String path,
            @AuthenticationPrincipal SecurityUser currentUser
            ) {
        ResourceResponseDto dto = resourcesService.getResourceInfo(currentUser.getId(), path);

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResource(
            @Pattern(regexp = "^(/|.*[^/].*/?)$", message = "Resource path must be non-empty and end with /") @RequestParam String path,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        resourcesService.deleteResource(currentUser.getId(), path);

        return ResponseEntity.noContent().build();
    }
}