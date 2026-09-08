package io.github.XanderGI.service;

import io.github.XanderGI.dto.request.UserRequestDto;
import org.springframework.security.core.Authentication;

public interface AuthService {

    Authentication signUp(UserRequestDto dto);

    Authentication signIn(UserRequestDto dto);
}