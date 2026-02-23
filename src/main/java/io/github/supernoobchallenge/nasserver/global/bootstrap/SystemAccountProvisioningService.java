package io.github.supernoobchallenge.nasserver.global.bootstrap;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.repository.UserPermissionRepository;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemAccountProvisioningService {
    private static final long SYSTEM_USER_ID = 1L;

    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final FilePermissionKeyRepository filePermissionKeyRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void provisionOrSync(SystemAccountProperties properties) {
        userRepository.findById(SYSTEM_USER_ID)
                .ifPresentOrElse(
                        systemUser -> syncCredentialIfRequired(systemUser, properties),
                        () -> createSystemUser(properties)
                );
    }

    private void syncCredentialIfRequired(User systemUser, SystemAccountProperties properties) {
        boolean updated = false;

        if (!properties.getLoginId().equals(systemUser.getLoginId())) {
            systemUser.changeLoginId(properties.getLoginId());
            updated = true;
        }

        if (!passwordEncoder.matches(properties.getPassword(), systemUser.getPassword())) {
            systemUser.changePassword(passwordEncoder.encode(properties.getPassword()));
            updated = true;
        }

        if (updated) {
            log.info("시스템 계정(user_id={}) 자격 정보를 system.properties 기준으로 동기화했습니다.", SYSTEM_USER_ID);
        }
    }

    private void createSystemUser(SystemAccountProperties properties) {
        if (userRepository.existsByLoginId(properties.getLoginId())) {
            throw new IllegalStateException(
                    String.format("system.account.login-id('%s')를 사용하는 계정이 이미 존재하여 user_id=1 시스템 계정을 생성할 수 없습니다.", properties.getLoginId())
            );
        }

        String systemEmail = properties.resolveEmail();
        if (userRepository.existsByEmail(systemEmail)) {
            throw new IllegalStateException(
                    String.format("system.account.email('%s')를 사용하는 계정이 이미 존재하여 user_id=1 시스템 계정을 생성할 수 없습니다.", systemEmail)
            );
        }

        FilePermissionKey filePermissionKey = filePermissionKeyRepository.save(
                FilePermissionKey.builder()
                        .ownerType(FilePermissionKey.OwnerType.USER)
                        .parentPermission(null)
                        .build()
        );

        if (filePermissionKey.getId() == null) {
            throw new IllegalStateException("시스템 계정 생성을 위한 file_permission_key 생성에 실패했습니다.");
        }

        int insertedUser = userRepository.insertSystemUser(
                SYSTEM_USER_ID,
                filePermissionKey.getId(),
                properties.getLoginId(),
                passwordEncoder.encode(properties.getPassword()),
                systemEmail,
                SYSTEM_USER_ID
        );

        if (insertedUser != 1) {
            throw new IllegalStateException("user_id=1 시스템 계정 생성에 실패했습니다.");
        }

        int insertedPermission = userPermissionRepository.insertSystemUserPermission(
                SYSTEM_USER_ID,
                false,
                false,
                true,
                false,
                SYSTEM_USER_ID
        );
        if (insertedPermission != 1) {
            throw new IllegalStateException("user_id=1 시스템 계정 권한 생성에 실패했습니다.");
        }

        log.info("system.properties 기준으로 user_id=1 시스템 계정을 자동 생성했습니다.");
    }
}
