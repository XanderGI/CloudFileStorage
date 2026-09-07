package io.github.XanderGI.service.impl;

import io.github.XanderGI.dto.request.UserRequestDto;
import io.github.XanderGI.entity.User;
import io.github.XanderGI.exception.UserAlreadyExistException;
import io.github.XanderGI.repository.UserRepository;
import io.github.XanderGI.service.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final String LOGIN_OCCUPIED_ERROR = "User with this username already exist, change username";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public Authentication signUp(UserRequestDto dto) {
        userRepository.findByUsername(dto.username())
                .ifPresent(user -> {
                    throw new UserAlreadyExistException(LOGIN_OCCUPIED_ERROR);
                });

        String passwordHash = passwordEncoder.encode(dto.password());

        userRepository.save(new User(dto.username(), passwordHash));

        log.info("New user registered: {}", dto.username());

        return authenticateUser(dto.username(), dto.password());
    }

    @Override
    public Authentication signIn(UserRequestDto dto) {
        return authenticateUser(dto.username(), dto.password());
    }

    private Authentication authenticateUser(String username, String password) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);

        return authenticationManager.authenticate(token);
    }
}