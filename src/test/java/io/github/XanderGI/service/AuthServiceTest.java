package io.github.XanderGI.service;

import io.github.XanderGI.TestcontainersConfiguration;
import io.github.XanderGI.dto.request.UserRequestDto;
import io.github.XanderGI.entity.User;
import io.github.XanderGI.exception.UserAlreadyExistException;
import io.github.XanderGI.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
public class AuthServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserRequestDto dto;
    private User user;

    @BeforeEach
    public void setUp() {
        dto = new UserRequestDto("testUsername", "testPassword");
        user = new User("testUsername", passwordEncoder.encode("testPassword"));
    }

    @Test
    public void shouldCreateNewUser() {
        Authentication auth = authService.signUp(dto);

        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getName()).isEqualTo(dto.username());
        assertThat(userRepository.findByUsername(dto.username()))
                .isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u.getUsername()).isEqualTo(dto.username());
                    assertThat(passwordEncoder.matches(dto.password(), u.getPassword())).isTrue();
                });
    }

    @Test
    public void shouldFailSignUpWhenUserAlreadyExists() {
        Authentication auth = authService.signUp(dto);

        assertThat(auth).isNotNull();
        assertThatThrownBy(() -> authService.signUp(dto))
                .isInstanceOf(UserAlreadyExistException.class)
                .hasMessageContaining("already exist");
    }

    @Test
    public void shouldSignInSuccessfullyWhenCredentialsAreValid() {
        userRepository.save(user);

        Authentication auth = authService.signIn(dto);

        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getName()).isEqualTo(dto.username());
    }

    @Test
    public void shouldFailSignInWhenPasswordIsIncorrect() {
        userRepository.save(user);
        UserRequestDto invalidDto = new UserRequestDto("testUsername", "invalidPassword");

        assertThatThrownBy(() -> authService.signIn(invalidDto))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void shouldFailSignInWhenUserDoesNotExist() {
        assertThatThrownBy(() -> authService.signIn(dto))
                .isInstanceOf(BadCredentialsException.class);
    }
}