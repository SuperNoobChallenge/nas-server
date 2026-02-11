package io.github.supernoobchallenge.nasserver.user.service;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.entity.UserPermission;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final FilePermissionKeyRepository filePermissionKeyRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long register(String loginId, String rawPassword, String email, Long inviterId) {
        validateRegisterInput(loginId, rawPassword, email);

        if (userRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 사용 중인 loginId입니다.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User inviter = null;
        if (inviterId != null) {
            inviter = userRepository.findById(inviterId)
                    .orElseThrow(() -> new IllegalArgumentException("inviter가 존재하지 않습니다."));
        }

        FilePermissionKey permissionKey = FilePermissionKey.builder()
                .ownerType(FilePermissionKey.OwnerType.USER)
                .parentPermission(inviter == null ? null : inviter.getFilePermission())
                .build();
        filePermissionKeyRepository.save(permissionKey);

        User user = User.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(rawPassword))
                .email(email)
                .filePermission(permissionKey)
                .inviter(inviter)
                .build();

        UserPermission userPermission = UserPermission.builder()
                .user(user)
                .canInvitePerson(false)
                .canShareFile(false)
                .canStorePersonal(true)
                .canCreateGroup(false)
                .build();
        user.assignPermission(userPermission);
        userRepository.save(user);

        return user.getId();
    }

    @Transactional
    public void changePassword(Long userId, String currentRawPassword, String newRawPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (currentRawPassword == null || currentRawPassword.isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호는 비어있을 수 없습니다.");
        }
        if (newRawPassword == null || newRawPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호는 비어있을 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        if (!passwordEncoder.matches(currentRawPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(newRawPassword, user.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호와 동일합니다.");
        }

        user.changePassword(passwordEncoder.encode(newRawPassword));
    }

    private void validateRegisterInput(String loginId, String rawPassword, String email) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId는 비어있을 수 없습니다.");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 비어있을 수 없습니다.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email은 비어있을 수 없습니다.");
        }
    }
}
