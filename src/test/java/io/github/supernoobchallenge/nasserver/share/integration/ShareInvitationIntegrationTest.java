package io.github.supernoobchallenge.nasserver.share.integration;

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
class ShareInvitationIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShareLinkRepository shareLinkRepository;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("초대 링크를 생성하고 링크로 회원가입하면 inviter가 연결되고 사용횟수가 증가한다")
    void createInviteLink_AndInviteRegister_Succeeds() throws Exception {
        userService.register("inviter_api", "pass-1234", "inviter_api@test.com");
        MockHttpSession inviterSession = login("inviter_api", "pass-1234");

        String createBody = """
                {
                  "name": "team-invite",
                  "validHours": 24,
                  "maxUseCount": 2,
                  "linkPassword": "link-pass"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/share-links/invites")
                        .session(inviterSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shareUuid").isString())
                .andReturn();

        String shareUuid = readField(createResult, "shareUuid");

        String registerBody = """
                {
                  "shareUuid":"%s",
                  "linkPassword":"link-pass",
                  "loginId":"invited_api",
                  "password":"pass-9999",
                  "email":"invited_api@test.com"
                }
                """.formatted(shareUuid);

        MvcResult registerResult = mockMvc.perform(post("/api/users/invite-register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andReturn();

        Long invitedUserId = Long.parseLong(readField(registerResult, "userId"));
        User invited = userRepository.findById(invitedUserId).orElseThrow();
        ShareLink link = shareLinkRepository.findByShareUuid(shareUuid).orElseThrow();
        Long inviterId = userRepository.findByLoginId("inviter_api").orElseThrow().getId();

        assertThat(invited.getInviter()).isNotNull();
        assertThat(invited.getInviter().getId()).isEqualTo(inviterId);
        assertThat(link.getCurrentUseCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("사용 횟수를 초과한 초대 링크는 회원가입에 사용할 수 없다")
    void inviteRegister_WhenUseCountExceeded_ReturnsBadRequest() throws Exception {
        userService.register("inviter_limit", "pass-1234", "inviter_limit@test.com");
        MockHttpSession inviterSession = login("inviter_limit", "pass-1234");

        String createBody = """
                {
                  "name": "limit-invite",
                  "validHours": 24,
                  "maxUseCount": 1
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/share-links/invites")
                        .session(inviterSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        String shareUuid = readField(createResult, "shareUuid");

        String firstRegister = """
                {
                  "shareUuid":"%s",
                  "loginId":"invited_limit_1",
                  "password":"pass-9999",
                  "email":"invited_limit_1@test.com"
                }
                """.formatted(shareUuid);

        mockMvc.perform(post("/api/users/invite-register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRegister))
                .andExpect(status().isCreated());

        String secondRegister = """
                {
                  "shareUuid":"%s",
                  "loginId":"invited_limit_2",
                  "password":"pass-9999",
                  "email":"invited_limit_2@test.com"
                }
                """.formatted(shareUuid);

        mockMvc.perform(post("/api/users/invite-register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondRegister))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("사용 가능 횟수를 초과한 초대 링크입니다."));
    }

    @Test
    @DisplayName("로그인 없이 초대 링크 생성 요청 시 403을 반환한다")
    void createInviteLink_WithoutLogin_ReturnsForbidden() throws Exception {
        String createBody = """
                {
                  "name": "anonymous-invite",
                  "validHours": 24,
                  "maxUseCount": 1
                }
                """;

        mockMvc.perform(post("/api/share-links/invites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());
    }

    private String readField(MvcResult result, String fieldName) throws Exception {
        String body = result.getResponse().getContentAsString();
        String pattern = "\"" + fieldName + "\":";
        int start = body.indexOf(pattern);
        if (start < 0) {
            throw new IllegalArgumentException("응답에서 필드를 찾을 수 없습니다. field=" + fieldName);
        }
        int valueStart = start + pattern.length();
        while (valueStart < body.length() && Character.isWhitespace(body.charAt(valueStart))) {
            valueStart++;
        }
        if (body.charAt(valueStart) == '"') {
            int end = body.indexOf('"', valueStart + 1);
            return body.substring(valueStart + 1, end);
        }
        int end = valueStart;
        while (end < body.length() && Character.isDigit(body.charAt(end))) {
            end++;
        }
        return body.substring(valueStart, end);
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
