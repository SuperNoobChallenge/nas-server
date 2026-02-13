package io.github.supernoobchallenge.nasserver.user.service;

import io.github.supernoobchallenge.nasserver.share.entity.ShareLink;
import io.github.supernoobchallenge.nasserver.share.repository.ShareLinkRepository;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final int PASSWORD_RESET_VALID_MINUTES = 30;

    private final UserRepository userRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailService passwordResetMailService;

    @Value("${app.password-reset.link-base-url:http://localhost:8080/password-reset}")
    private String passwordResetLinkBaseUrl;

    @Transactional
    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email은 비어있을 수 없습니다.");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();
        if (user.isDeleted()) {
            return;
        }

        invalidateActiveResetLinks(user.getId());

        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expirationDate = LocalDateTime.now().plusMinutes(PASSWORD_RESET_VALID_MINUTES);
        ShareLink resetLink = shareLinkRepository.save(ShareLink.createPasswordResetLink(user, token, expirationDate));
        passwordResetMailService.sendPasswordResetEmail(user.getEmail(), buildResetLinkUrl(resetLink.getShareUuid()));
    }

    @Transactional
    public void confirmPasswordReset(String token, String newRawPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token은 비어있을 수 없습니다.");
        }
        if (newRawPassword == null || newRawPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호는 비어있을 수 없습니다.");
        }

        ShareLink resetLink = shareLinkRepository.findByShareUuid(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 비밀번호 재설정 링크입니다."));
        validateResetLink(resetLink);

        User user = resetLink.getUser();
        if (user.isDeleted()) {
            throw new IllegalArgumentException("비활성화된 사용자입니다.");
        }
        if (passwordEncoder.matches(newRawPassword, user.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호와 동일합니다.");
        }

        user.changePassword(passwordEncoder.encode(newRawPassword));
        resetLink.increaseUseCount();
        resetLink.delete();
    }

    private void invalidateActiveResetLinks(Long userId) {
        shareLinkRepository.findAllByUser_IdAndLinkTypeAndDeletedAtIsNull(userId, ShareLink.LINK_TYPE_PASSWORD_RESET)
                .forEach(ShareLink::delete);
    }

    private void validateResetLink(ShareLink resetLink) {
        if (resetLink.isDeleted()) {
            throw new IllegalArgumentException("이미 사용된 비밀번호 재설정 링크입니다.");
        }
        if (!resetLink.isPasswordResetLink()) {
            throw new IllegalArgumentException("비밀번호 재설정 링크가 아닙니다.");
        }
        if (resetLink.isExpired(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료된 비밀번호 재설정 링크입니다.");
        }
        if (resetLink.isExhausted()) {
            throw new IllegalArgumentException("이미 사용된 비밀번호 재설정 링크입니다.");
        }
    }

    private String buildResetLinkUrl(String token) {
        return passwordResetLinkBaseUrl + "?token=" + token;
    }
}
