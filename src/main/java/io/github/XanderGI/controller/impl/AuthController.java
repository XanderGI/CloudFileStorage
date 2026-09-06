package io.github.XanderGI.controller.impl;

import io.github.XanderGI.controller.AuthControllerApi;
import io.github.XanderGI.dto.request.UserRequestDto;
import io.github.XanderGI.dto.response.UserResponseDto;
import io.github.XanderGI.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// todo: сделать маппер для dto

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthControllerApi {
    private final AuthService authService;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/sign-up")
    public ResponseEntity<UserResponseDto> signUp(
            @Valid @RequestBody UserRequestDto dto,
            HttpServletRequest req,
            HttpServletResponse resp) {

        Authentication auth = authService.signUp(dto);

        setupSecurityContext(auth, req, resp);

        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponseDto(auth.getName()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<UserResponseDto> signIn(
            @Valid @RequestBody UserRequestDto dto,
            HttpServletRequest req,
            HttpServletResponse resp) {

        Authentication auth = authService.signIn(dto);

        setupSecurityContext(auth, req, resp);

        return ResponseEntity.ok(new UserResponseDto(auth.getName()));
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(HttpServletRequest req, HttpServletResponse resp) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

            logoutHandler.setInvalidateHttpSession(true);

            logoutHandler.logout(req, resp, auth);
        }

        return ResponseEntity.noContent().build();
    }

    private void setupSecurityContext(Authentication auth, HttpServletRequest req, HttpServletResponse resp) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, req, resp);
    }
}