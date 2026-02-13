package io.github.supernoobchallenge.nasserver.user.integration;

import io.github.supernoobchallenge.nasserver.share.entity.ShareLink;
import io.github.supernoobchallenge.nasserver.share.repository.ShareLinkRepository;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ShareLinkRepository shareLinkRepository;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("직접 회원가입 API는 허용되지 않는다")
    void register_IsNotAllowed() throws Exception {
        String body = """
                {
                  "loginId":"api_user_1",
                  "password":"pass-1234",
                  "email":"api_user_1@test.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비밀번호 변경 API 성공 시 204를 반환하고 비밀번호가 변경된다")
    void changePassword_ReturnsNoContentAndUpdatesPassword() throws Exception {
        Long userId = userService.register("api_user_2", "old-pass", "api_user_2@test.com");
        MockHttpSession session = login("api_user_2", "old-pass");

        String body = """
                {
                  "currentPassword":"old-pass",
                  "newPassword":"new-pass"
                }
                """;

        mockMvc.perform(patch("/api/users/{userId}/password", userId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        User updated = userRepository.findById(userId).orElseThrow();
        assertThat(passwordEncoder.matches("new-pass", updated.getPassword())).isTrue();
    }

    @Test
    @DisplayName("직접 회원가입 API는 입력값과 무관하게 허용되지 않는다")
    void register_WhenInvalidInput_StillNotAllowed() throws Exception {
        String body = """
                {
                  "loginId":" ",
                  "password":"pass-1234",
                  "email":"api_user_3@test.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("로그인 없이 비밀번호 변경 API 요청 시 403을 반환한다")
    void changePassword_WithoutLogin_ReturnsForbidden() throws Exception {
        Long userId = userService.register("api_user_4", "old-pass", "api_user_4@test.com");

        String body = """
                {
                  "currentPassword":"old-pass",
                  "newPassword":"new-pass"
                }
                """;

        mockMvc.perform(patch("/api/users/{userId}/password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 API는 로그인 없이 호출할 수 있고 재설정 링크를 생성한다")
    void requestPasswordReset_WithoutLogin_ReturnsNoContent() throws Exception {
        Long userId = userService.register("reset_user_1", "old-pass", "reset_user_1@test.com");

        String body = """
                {
                  "email":"reset_user_1@test.com"
                }
                """;

        mockMvc.perform(post("/api/users/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        ShareLink resetLink = shareLinkRepository
                .findTopByUser_IdAndLinkTypeAndDeletedAtIsNullOrderByCreatedAtDesc(
                        userId,
                        ShareLink.LINK_TYPE_PASSWORD_RESET
                )
                .orElseThrow();
        assertThat(resetLink.isPasswordResetLink()).isTrue();
        assertThat(resetLink.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("비밀번호 재설정 확정 API는 유효한 토큰이면 비밀번호를 변경한다")
    void confirmPasswordReset_WithValidToken_UpdatesPassword() throws Exception {
        Long userId = userService.register("reset_user_2", "old-pass", "reset_user_2@test.com");

        mockMvc.perform(post("/api/users/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"reset_user_2@test.com"}
                                """))
                .andExpect(status().isNoContent());

        ShareLink resetLink = shareLinkRepository
                .findTopByUser_IdAndLinkTypeAndDeletedAtIsNullOrderByCreatedAtDesc(
                        userId,
                        ShareLink.LINK_TYPE_PASSWORD_RESET
                )
                .orElseThrow();

        String confirmBody = """
                {
                  "token":"%s",
                  "newPassword":"new-pass"
                }
                """.formatted(resetLink.getShareUuid());

        mockMvc.perform(post("/api/users/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isNoContent());

        User updatedUser = userRepository.findById(userId).orElseThrow();
        ShareLink consumedLink = shareLinkRepository.findByShareUuid(resetLink.getShareUuid()).orElseThrow();
        assertThat(passwordEncoder.matches("new-pass", updatedUser.getPassword())).isTrue();
        assertThat(consumedLink.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("비밀번호 재설정 확정 API는 잘못된 토큰이면 400을 반환한다")
    void confirmPasswordReset_WithInvalidToken_ReturnsBadRequest() throws Exception {
        String body = """
                {
                  "token":"invalid-token",
                  "newPassword":"new-pass"
                }
                """;

        mockMvc.perform(post("/api/users/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession login(String loginId, String password) throws Exception {
        String body = """
                {"loginId":"%s","password":"%s"}
                """.formatted(loginId, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
