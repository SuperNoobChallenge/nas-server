package io.github.supernoobchallenge.nasserver.user.service;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.entity.UserPermission;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey.OwnerType.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FilePermissionKeyRepository filePermissionKeyRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("register는 비밀번호를 해시하여 저장하고 기본 권한을 생성한다")
    void register_HashesPasswordAndCreatesDefaultPermission() {
        when(userRepository.existsByLoginId("user1")).thenReturn(false);
        when(userRepository.existsByEmail("u1@test.com")).thenReturn(false);
        when(passwordEncoder.encode("raw-pass")).thenReturn("hashed-pass");

        userService.register("user1", "raw-pass", "u1@test.com", null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("hashed-pass", savedUser.getPassword());
        assertEquals("user1", savedUser.getLoginId());
        assertEquals("u1@test.com", savedUser.getEmail());
        UserPermission permission = savedUser.getUserPermission();
        assertEquals(false, permission.isCanInvitePerson());
        assertEquals(false, permission.isCanShareFile());
        assertEquals(true, permission.isCanStorePersonal());
        assertEquals(false, permission.isCanCreateGroup());

        ArgumentCaptor<FilePermissionKey> keyCaptor = ArgumentCaptor.forClass(FilePermissionKey.class);
        verify(filePermissionKeyRepository).save(keyCaptor.capture());
        assertEquals(USER, keyCaptor.getValue().getOwnerType());
    }

    @Test
    @DisplayName("유저가 유저를 초대해 등록하면 inviter와 parentPermission이 연결된다")
    void register_WithInviter_SetsInviterAndParentPermission() {
        FilePermissionKey inviterKey = FilePermissionKey.builder().ownerType(USER).build();
        User inviter = User.builder()
                .loginId("inviter")
                .password("inviter-hash")
                .email("inviter@test.com")
                .filePermission(inviterKey)
                .build();

        when(userRepository.existsByLoginId("child")).thenReturn(false);
        when(userRepository.existsByEmail("child@test.com")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(inviter));
        when(passwordEncoder.encode("child-pass")).thenReturn("child-hash");

        userService.register("child", "child-pass", "child@test.com", 10L);

        ArgumentCaptor<FilePermissionKey> keyCaptor = ArgumentCaptor.forClass(FilePermissionKey.class);
        verify(filePermissionKeyRepository).save(keyCaptor.capture());
        FilePermissionKey savedKey = keyCaptor.getValue();
        assertEquals(inviterKey, savedKey.getParentPermission());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(inviter, savedUser.getInviter());
        assertEquals("child-hash", savedUser.getPassword());
    }

    @Test
    @DisplayName("changePassword는 현재 비밀번호 검증 후 새 비밀번호를 해시 저장한다")
    void changePassword_ValidatesCurrentAndStoresHashed() {
        FilePermissionKey key = FilePermissionKey.builder().ownerType(USER).build();
        User user = User.builder()
                .loginId("user1")
                .password("old-hash")
                .email("u1@test.com")
                .filePermission(key)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-raw", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-raw", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-raw")).thenReturn("new-hash");

        userService.changePassword(1L, "old-raw", "new-raw");

        assertEquals("new-hash", user.getPassword());
    }

    @Test
    @DisplayName("changePassword에서 현재 비밀번호가 틀리면 예외를 던진다")
    void changePassword_WhenCurrentPasswordMismatch_Throws() {
        FilePermissionKey key = FilePermissionKey.builder().ownerType(USER).build();
        User user = User.builder()
                .loginId("user1")
                .password("old-hash")
                .email("u1@test.com")
                .filePermission(key)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(1L, "wrong", "new-raw"));
        verify(passwordEncoder, never()).encode(any());
    }
}
