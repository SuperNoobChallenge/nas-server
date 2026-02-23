package io.github.supernoobchallenge.nasserver.file.core.integration;

import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualDirectoryRepository;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VirtualDirectoryPageControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VirtualDirectoryRepository virtualDirectoryRepository;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("로그인 페이지는 비로그인 상태에서 접근 가능하다")
    void loginPage_WithoutLogin_ReturnsOk() throws Exception {
        mockMvc.perform(get("/web/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("웹 로그인 성공 시 디렉터리 페이지로 리다이렉트된다")
    void webLogin_WithValidUser_RedirectsToDirectories() throws Exception {
        userService.register("web_user_1", "pass-1234", "web_user_1@test.com");

        mockMvc.perform(post("/web/login")
                        .param("loginId", "web_user_1")
                        .param("password", "pass-1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/directories"));
    }

    @Test
    @DisplayName("디렉터리 페이지 폼으로 생성한 디렉터리는 실제 저장된다")
    void createDirectory_UsingWebForm_PersistsDirectory() throws Exception {
        Long userId = userService.register("web_user_2", "pass-1234", "web_user_2@test.com");
        MockHttpSession session = loginViaWeb("web_user_2", "pass-1234");

        mockMvc.perform(post("/web/directories/create")
                        .session(session)
                        .param("name", "web-root")
                        .param("readLevel", "0")
                        .param("writeLevel", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/directories"));

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(virtualDirectoryRepository.findActiveChildren(user.getFilePermission().getId(), null))
                .extracting("name")
                .contains("web-root");
    }

    private MockHttpSession loginViaWeb(String loginId, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/web/login")
                        .param("loginId", loginId)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
