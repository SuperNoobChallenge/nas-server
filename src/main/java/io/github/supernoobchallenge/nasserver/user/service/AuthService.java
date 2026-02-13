package io.github.supernoobchallenge.nasserver.user.service;

import io.github.supernoobchallenge.nasserver.user.dto.LoginResponse;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    public static final String SESSION_USER_ID = "LOGIN_USER_ID";
    public static final String SESSION_LOGIN_ID = "LOGIN_ID";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(String loginId, String rawPassword, HttpServletRequest request) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId는 비어있을 수 없습니다.");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("password는 비어있을 수 없습니다.");
        }

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (user.isDeleted()) {
            throw new IllegalArgumentException("비활성화된 사용자입니다.");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_LOGIN_ID, user.getLoginId());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getId(), null, null);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return new LoginResponse(user.getId(), user.getLoginId(), user.getEmail());
    }

    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
