package io.github.XanderGI.controller;

import io.github.XanderGI.dto.UserResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/user/me")
    public ResponseEntity<UserResponseDto> getMe(Principal principal) {
        return ResponseEntity.ok(new UserResponseDto(principal.getName()));
    }
}