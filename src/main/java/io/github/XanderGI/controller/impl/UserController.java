package io.github.XanderGI.controller.impl;

import io.github.XanderGI.controller.UserControllerApi;
import io.github.XanderGI.dto.UserResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api")
public class UserController implements UserControllerApi {

    @GetMapping("/user/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(Principal principal) {
        return ResponseEntity.ok(new UserResponseDto(principal.getName()));
    }
}