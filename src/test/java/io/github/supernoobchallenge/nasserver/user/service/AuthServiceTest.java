package io.github.supernoobchallenge.nasserver.user.service;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.user.dto.LoginResponse;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey.OwnerType.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login 성공 시 세션에 로그인 사용자 정보를 저장한다")
    void login_Success_StoresSessionAttributes() {
        FilePermissionKey key = FilePermissionKey.builder().ownerType(USER).build();
        User user = User.builder()
                .loginId("tester")
                .password("hashed")
                .email("test@test.com")
                .filePermission(key)
                .build();

        when(userRepository.findByLoginId("tester")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain", "hashed")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginResponse response = authService.login("tester", "plain", request);

        assertEquals("tester", response.loginId());
        assertEquals("test@test.com", response.email());
        assertNotNull(request.getSession(false));
        assertEquals(user.getId(), request.getSession(false).getAttribute(AuthService.SESSION_USER_ID));
        assertEquals("tester", request.getSession(false).getAttribute(AuthService.SESSION_LOGIN_ID));
    }

    @Test
    @DisplayName("login 실패(비밀번호 불일치) 시 예외를 던진다")
    void login_Fail_WhenPasswordMismatch() {
        FilePermissionKey key = FilePermissionKey.builder().ownerType(USER).build();
        User user = User.builder()
                .loginId("tester")
                .password("hashed")
                .email("test@test.com")
                .filePermission(key)
                .build();

        when(userRepository.findByLoginId("tester")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(IllegalArgumentException.class, () -> authService.login("tester", "wrong", request));
    }

    @Test
    @DisplayName("logout 호출 시 기존 세션을 무효화한다")
    void logout_InvalidateSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = spy(new MockHttpSession());
        request.setSession(session);

        authService.logout(request);

        verify(session).invalidate();
    }
}
