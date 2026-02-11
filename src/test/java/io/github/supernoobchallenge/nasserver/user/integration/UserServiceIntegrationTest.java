package io.github.supernoobchallenge.nasserver.user.integration;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.entity.UserPermission;
import io.github.supernoobchallenge.nasserver.user.repository.UserPermissionRepository;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import io.github.supernoobchallenge.nasserver.user.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private FilePermissionKeyRepository filePermissionKeyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원 가입 시 User, UserPermission, FilePermissionKey가 함께 저장된다")
    void register_PersistsUserWithPermissionAndFilePermissionKey() {
        Long userId = userService.register("integration_user_1", "plain-1234", "integration1@test.com", null);

        User user = userRepository.findById(userId).orElseThrow();
        UserPermission permission = userPermissionRepository.findById(userId).orElseThrow();
        FilePermissionKey key = filePermissionKeyRepository.findById(user.getFilePermission().getId()).orElseThrow();

        assertThat(user.getLoginId()).isEqualTo("integration_user_1");
        assertThat(passwordEncoder.matches("plain-1234", user.getPassword())).isTrue();
        assertThat(permission.isCanInvitePerson()).isFalse();
        assertThat(permission.isCanShareFile()).isFalse();
        assertThat(permission.isCanStorePersonal()).isTrue();
        assertThat(permission.isCanCreateGroup()).isFalse();
        assertThat(key.getOwnerType()).isEqualTo(FilePermissionKey.OwnerType.USER);
    }

    @Test
    @DisplayName("비밀번호 변경 시 DB에 해시값으로 갱신된다")
    void changePassword_UpdatesHashedPassword() {
        Long userId = userService.register("integration_user_2", "old-pass", "integration2@test.com", null);

        userService.changePassword(userId, "old-pass", "new-pass");

        User updated = userRepository.findById(userId).orElseThrow();
        assertThat(passwordEncoder.matches("new-pass", updated.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("old-pass", updated.getPassword())).isFalse();
    }
}
