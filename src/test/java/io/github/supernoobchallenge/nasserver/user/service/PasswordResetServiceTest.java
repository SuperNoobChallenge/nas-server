package io.github.supernoobchallenge.nasserver.user.service;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.share.entity.ShareLink;
import io.github.supernoobchallenge.nasserver.share.repository.ShareLinkRepository;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey.OwnerType.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ShareLinkRepository shareLinkRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordResetMailService passwordResetMailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    @DisplayName("requestPasswordReset은 활성 사용자에게 재설정 링크를 생성하고 메일 전송을 요청한다")
    void requestPasswordReset_CreatesResetLinkAndSendsMail() {
        FilePermissionKey key = FilePermissionKey.builder().ownerType(USER).build();
        User user = User.builder()
                .loginId("reset-user")
                .password("old-hash")
                .email("reset-user@test.com")
                .filePermission(key)
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);
        ReflectionTestUtils.setField(passwordResetService, "passwordResetLinkBaseUrl", "https://nas.local/reset");

        when(userRepository.findByEmail("reset-user@test.com")).thenReturn(Optional.of(user));
        when(shareLinkRepository.findAllByUser_IdAndLinkTypeAndDeletedAtIsNull(10L, ShareLink.LINK_TYPE_PASSWORD_RESET))
                .thenReturn(List.of());
        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.requestPasswordReset("reset-user@test.com");

        ArgumentCaptor<ShareLink> linkCaptor = ArgumentCaptor.forClass(ShareLink.class);
        verify(shareLinkRepository).save(linkCaptor.capture());
        ShareLink savedLink = linkCaptor.getValue();
        assertEquals(ShareLink.LINK_TYPE_PASSWORD_RESET, savedLink.getLinkType());
        assertEquals(1, savedLink.getMaxUseCount());
        assertEquals(0, savedLink.getCurrentUseCount());

        verify(passwordResetMailService).sendPasswordResetEmail(
                org.mockito.ArgumentMatchers.eq("reset-user@test.com"),
                contains("https://nas.local/reset?token=")
        );
    }

    @Test
    @DisplayName("requestPasswordReset은 존재하지 않는 이메일이면 아무 작업도 하지 않는다")
    void requestPasswordReset_WhenUserNotFound_NoOp() {
        when(userRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset("none@test.com");

        verify(shareLinkRepository, never()).save(any());
        verify(passwordResetMailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    @DisplayName("confirmPasswordReset은 유효한 토큰이면 비밀번호를 변경하고 링크를 소모 처리한다")
    void confirmPasswordReset_ChangesPasswordAndConsumesLink() {
        FilePermissionKey key = FilePermissionKey.builder().ownerType(USER).build();
        User user = User.builder()
                .loginId("reset-user")
                .password("old-hash")
                .email("reset-user@test.com")
                .filePermission(key)
                .build();
        ShareLink resetLink = ShareLink.createPasswordResetLink(
                user,
                "token-1",
                LocalDateTime.now().plusMinutes(30)
        );

        when(shareLinkRepository.findByShareUuid("token-1")).thenReturn(Optional.of(resetLink));
        when(passwordEncoder.matches("new-pass", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");

        passwordResetService.confirmPasswordReset("token-1", "new-pass");

        assertEquals("new-hash", user.getPassword());
        assertEquals(1, resetLink.getCurrentUseCount());
        assertEquals(true, resetLink.isDeleted());
    }

    @Test
    @DisplayName("confirmPasswordReset은 재설정 링크가 아니면 예외를 던진다")
    void confirmPasswordReset_WhenNotResetLink_Throws() {
        FilePermissionKey key = FilePermissionKey.builder().ownerType(USER).build();
        User user = User.builder()
                .loginId("reset-user")
                .password("old-hash")
                .email("reset-user@test.com")
                .filePermission(key)
                .build();
        ShareLink inviteLink = ShareLink.createInviteLink(
                user,
                "token-2",
                LocalDateTime.now().plusMinutes(30),
                null,
                1,
                "invite"
        );

        when(shareLinkRepository.findByShareUuid("token-2")).thenReturn(Optional.of(inviteLink));

        assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.confirmPasswordReset("token-2", "new-pass"));
        verify(passwordEncoder, never()).encode(any());
    }
}
