package io.github.XanderGI.integration;

import io.github.XanderGI.TestcontainersConfiguration;
import io.github.XanderGI.dto.UserRequestDto;
import io.github.XanderGI.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
public class RedisSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    @MockitoBean
    private AuthService authService;

    private UserRequestDto validDto;
    private Authentication mockAuth;

    @BeforeEach
    public void setUp() {
        validDto = new UserRequestDto("testUsername", "testPassword");
        mockAuth = new UsernamePasswordAuthenticationToken(
                validDto.username(),
                validDto.password(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(authService.signUp(any())).thenReturn(mockAuth);
        when(authService.signIn(any())).thenReturn(mockAuth);
    }

    @Test
    public void shouldCreateRedisSessionOnSignUpAndSignInAndRemoveOnSignOut() throws Exception {
        MvcResult resultSignUp = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validDto)))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("SESSION"))
                .andReturn();

        Cookie signUpCookie = resultSignUp.getResponse().getCookie("SESSION");
        String sessionIdSignUp = new String(Base64.getDecoder().decode(signUpCookie.getValue()));

        assertThat(sessionRepository.findById(sessionIdSignUp)).isNotNull();

        mockMvc.perform(post("/api/auth/sign-out")
                        .cookie(signUpCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("SESSION", 0));

        assertThat(sessionRepository.findById(sessionIdSignUp)).isNull();

        MvcResult resultSignIn = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validDto)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("SESSION"))
                .andReturn();

        Cookie signInCookie = resultSignIn.getResponse().getCookie("SESSION");
        String sessionIdSignIn = new String(Base64.getDecoder().decode(signInCookie.getValue()));

        assertThat(sessionRepository.findById(sessionIdSignIn)).isNotNull();
    }
}