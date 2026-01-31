package io.github.supernoobchallenge.nasserver.repository;

import io.github.supernoobchallenge.nasserver.config.JpaConfig;
import io.github.supernoobchallenge.nasserver.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.entity.User;
import io.github.supernoobchallenge.nasserver.entity.UserPermission;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static io.github.supernoobchallenge.nasserver.entity.FilePermissionKey.OwnerType.USER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
@Import(JpaConfig.class)
class UserPermissionRepositoryTest {
    @Autowired private UserPermissionRepository userPermissionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FilePermissionKeyRepository filePermissionKeyRepository;
    @Autowired private EntityManager em;

    // 테스트 헬퍼: 유저 만들기
    private User createUser() {
        FilePermissionKey key = filePermissionKeyRepository.save(
                FilePermissionKey.builder().ownerType(USER).build()
        );
        User user = User.builder()
                .loginId("perm_test_user")
                .password("1234")
                .email("perm@test.com")
                .filePermission(key)
                .build();
        return userRepository.save(user);
    }

    @Test
    @DisplayName("1. Shared PK 테스트: 권한의 ID는 유저의 ID와 동일해야 한다")
    void sharedPkTest() {
        // given
        User user = createUser(); // 유저 먼저 생성 (ID가 생성됨)

        // when
        UserPermission permission = UserPermission.builder()
                .user(user) // 유저 연결 -> @MapsId가 작동해서 user의 ID를 가져옴
                .canShareFile(true)
                .canCreateGroup(false)
                .canInvitePerson(false)
                .canStorePersonal(true)
                .build();

        userPermissionRepository.save(permission);

        // 영속성 초기화
        em.flush();
        em.clear();

        // then
        UserPermission foundPerm = userPermissionRepository.findById(user.getId()).orElseThrow();

        System.out.println("유저 ID: " + user.getId());
        System.out.println("권한 ID: " + foundPerm.getId());

        // [핵심] 유저 ID로 권한을 조회할 수 있어야 함
        assertThat(foundPerm.getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("2. 값 검증: 설정한 Boolean 권한들이 정확히 조회되어야 한다")
    void booleanValueTest() {
        // given
        User user = createUser();
        UserPermission permission = UserPermission.builder()
                .user(user)
                .canShareFile(true)  // 1
                .canCreateGroup(false) // 0
                .canInvitePerson(true) // 1
                .canStorePersonal(false) // 0
                .build();
        userPermissionRepository.save(permission);

        em.flush();
        em.clear();

        // when
        UserPermission foundPerm = userPermissionRepository.findById(user.getId()).orElseThrow();

        // then
        assertThat(foundPerm.isCanShareFile()).isTrue();
        assertThat(foundPerm.isCanCreateGroup()).isFalse();
        assertThat(foundPerm.isCanInvitePerson()).isTrue();
        assertThat(foundPerm.isCanStorePersonal()).isFalse();
    }
}