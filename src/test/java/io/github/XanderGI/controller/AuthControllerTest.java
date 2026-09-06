package io.github.XanderGI.controller;

import io.github.XanderGI.controller.impl.AuthController;
import io.github.XanderGI.dto.request.UserRequestDto;
import io.github.XanderGI.exception.UserAlreadyExistException;
import io.github.XanderGI.security.SecurityConfiguration;
import io.github.XanderGI.service.AuthService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfiguration.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private AuthService authService;

    private UserRequestDto validDto;
    private Authentication mockAuth;

    @BeforeEach
    public void setUp() {
        validDto = new UserRequestDto("testUsername", "testPassword");
        mockAuth = new UsernamePasswordAuthenticationToken(validDto.username(), validDto.password());
    }

    @Test
    public void shouldReturn201AndCookiesWhenSignUpIsValid() throws Exception {
        when(authService.signUp(validDto)).thenReturn(mockAuth);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validDto)))
                .andExpect(status().isCreated())
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", Matchers.notNullValue()))
                .andExpect(jsonPath("$.username").value(validDto.username()));

        verify(authService, times(1)).signUp(validDto);
    }

    @Test
    public void shouldReturn409WhenUsernameIsTaken() throws Exception {
        when(authService.signUp(validDto)).thenThrow(new UserAlreadyExistException("User with this username already exist, change username"));

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validDto)))
                .andExpect(status().isConflict())
                .andExpect(cookie().doesNotExist("SESSION"))
                .andExpect(jsonPath("$.message").value("User with this username already exist, change username"));

        verify(authService, times(1)).signUp(validDto);
    }

    @ParameterizedTest
    @CsvSource({
            "valid, 123",
            "xand, 12345",
            "'', qwerty123",
            "xander, ''",
            "X@ander, secretPass",
            "xander, пароль"
    })
    public void shouldReturn400WhenSignUpDataIsInvalid(String username, String password) throws Exception {
        UserRequestDto invalidDto = new UserRequestDto(username, password);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(authService);
    }

    @Test
    public void shouldReturn200AndCookieWhenSignInIsValid() throws Exception {
        when(authService.signIn(validDto)).thenReturn(mockAuth);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validDto)))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", Matchers.notNullValue()))
                .andExpect(jsonPath("$.username").value("testUsername"));

        verify(authService, times(1)).signIn(validDto);
    }

    @Test
    public void shouldReturn401WhenCredentialsAreWrong() throws Exception {
        when(authService.signIn(validDto)).thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("SESSION"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        verify(authService, times(1)).signIn(validDto);
    }

    @Test
    @WithMockUser(username = "xander")
    public void shouldReturn204AndClearCookieWhenSignOutIsAuthorized() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturn401WhenSignOutIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}