package io.github.supernoobchallenge.nasserver.global.bootstrap;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.repository.UserPermissionRepository;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey.OwnerType.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemAccountProvisioningServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPermissionRepository userPermissionRepository;
    @Mock
    private FilePermissionKeyRepository filePermissionKeyRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SystemAccountProvisioningService provisioningService;

    @Test
    @DisplayName("user_id=1 시스템 계정이 있으면 loginId/password를 설정값으로 동기화한다")
    void provisionOrSync_WhenSystemUserExists_SyncsLoginIdAndPassword() {
        User systemUser = User.builder()
                .loginId("legacy-system")
                .password("old-hash")
                .email("system@test.com")
                .filePermission(FilePermissionKey.builder().ownerType(USER).build())
                .build();

        SystemAccountProperties properties = properties("system", "system-pass", "system@nasserver.local");

        when(userRepository.findById(1L)).thenReturn(Optional.of(systemUser));
        when(passwordEncoder.matches("system-pass", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("system-pass")).thenReturn("new-hash");

        provisioningService.provisionOrSync(properties);

        assertEquals("system", systemUser.getLoginId());
        assertEquals("new-hash", systemUser.getPassword());
    }

    @Test
    @DisplayName("user_id=1 시스템 계정이 없으면 자동 생성한다")
    void provisionOrSync_WhenSystemUserMissing_CreatesSystemUser() {
        FilePermissionKey savedPermissionKey = FilePermissionKey.builder()
                .ownerType(USER)
                .build();
        ReflectionTestUtils.setField(savedPermissionKey, "id", 10L);

        SystemAccountProperties properties = properties("system", "system-pass", "system@nasserver.local");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.existsByLoginId("system")).thenReturn(false);
        when(userRepository.existsByEmail("system@nasserver.local")).thenReturn(false);
        when(filePermissionKeyRepository.save(any(FilePermissionKey.class))).thenReturn(savedPermissionKey);
        when(passwordEncoder.encode("system-pass")).thenReturn("new-hash");
        when(userRepository.insertSystemUser(1L, 10L, "system", "new-hash", "system@nasserver.local", 1L))
                .thenReturn(1);
        when(userPermissionRepository.insertSystemUserPermission(1L, false, false, true, false, 1L))
                .thenReturn(1);

        provisioningService.provisionOrSync(properties);

        verify(userRepository).insertSystemUser(1L, 10L, "system", "new-hash", "system@nasserver.local", 1L);
        verify(userPermissionRepository).insertSystemUserPermission(1L, false, false, true, false, 1L);
    }

    @Test
    @DisplayName("시스템 계정이 이미 있고 비밀번호가 같으면 자동 생성을 시도하지 않는다")
    void provisionOrSync_WhenSystemUserExists_DoesNotCreateAgain() {
        User systemUser = User.builder()
                .loginId("system")
                .password("already-hash")
                .email("system@nasserver.local")
                .filePermission(FilePermissionKey.builder().ownerType(USER).build())
                .build();

        SystemAccountProperties properties = properties("system", "system-pass", "system@nasserver.local");

        when(userRepository.findById(1L)).thenReturn(Optional.of(systemUser));
        when(passwordEncoder.matches("system-pass", "already-hash")).thenReturn(true);

        provisioningService.provisionOrSync(properties);

        verify(userRepository, never()).insertSystemUser(1L, 10L, "system", "new-hash", "system@nasserver.local", 1L);
    }

    private SystemAccountProperties properties(String loginId, String password, String email) {
        SystemAccountProperties properties = new SystemAccountProperties();
        properties.setLoginId(loginId);
        properties.setPassword(password);
        properties.setEmail(email);
        return properties;
    }
}
