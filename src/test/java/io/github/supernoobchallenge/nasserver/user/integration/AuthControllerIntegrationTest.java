package io.github.supernoobchallenge.nasserver.user.integration;

import io.github.supernoobchallenge.nasserver.user.service.AuthService;
import io.github.supernoobchallenge.nasserver.user.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("로그인 성공 시 세션에 사용자 정보가 저장된다")
    void login_SetsSessionAttributes() throws Exception {
        userService.register("auth_integration_user", "pass-1234", "auth1@test.com");

        String body = """
                {"loginId":"auth_integration_user","password":"pass-1234"}
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("auth_integration_user"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(AuthService.SESSION_USER_ID)).isNotNull();
        assertThat(session.getAttribute(AuthService.SESSION_LOGIN_ID)).isEqualTo("auth_integration_user");
    }

    @Test
    @DisplayName("로그아웃 성공 시 세션이 무효화된다")
    void logout_InvalidatesSession() throws Exception {
        userService.register("auth_integration_user2", "pass-1234", "auth2@test.com");

        String loginBody = """
                {"loginId":"auth_integration_user2","password":"pass-1234"}
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
    }
}
