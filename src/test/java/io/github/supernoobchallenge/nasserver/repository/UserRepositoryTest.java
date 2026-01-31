package io.github.supernoobchallenge.nasserver.repository;

import io.github.supernoobchallenge.nasserver.config.JpaConfig;
import io.github.supernoobchallenge.nasserver.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static io.github.supernoobchallenge.nasserver.entity.FilePermissionKey.OwnerType.USER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
@Import(JpaConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired private FilePermissionKeyRepository filePermissionKeyRepository;
    @Autowired private EntityManager em;

    // 테스트용 헬퍼 메서드: 키 생성
    private FilePermissionKey createKey() {
        FilePermissionKey key = FilePermissionKey.builder().ownerType(USER).build();
        return filePermissionKeyRepository.save(key);
    }

    @Test
    @DisplayName("1. 커스텀 조회: findByLoginId로 유저를 찾을 수 있어야 한다")
    void findByLoginIdTest() {
        // given
        User user = User.builder()
                .loginId("test_user")
                .password("1234")
                .email("test@email.com")
                .filePermission(createKey()) // 필수 FK
                .build();
        userRepository.save(user);

        // when
        User foundUser = userRepository.findByLoginId("test_user").orElseThrow();

        // then
        assertThat(foundUser.getEmail()).isEqualTo("test@email.com");
        assertThat(foundUser.getId()).isNotNull();
    }

    @Test
    @DisplayName("2. 중복 방지: 이미 존재하는 LoginId로 가입하면 예외가 발생해야 한다")
    void uniqueConstraintTest() {
        // given
        User user1 = User.builder()
                .loginId("duplicate_id") // 중복될 ID
                .password("1234")
                .email("a@test.com")
                .filePermission(createKey())
                .build();
        userRepository.save(user1);

        // when & then
        // User2: ID는 같고 이메일은 다름
        User user2 = User.builder()
                .loginId("duplicate_id") // 중복 ID 시도!
                .password("5678")
                .email("b@test.com")
                .filePermission(createKey())
                .build();

        // DB 제약조건 위반 예외가 터져야 성공
        assertThatThrownBy(() -> userRepository.save(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("3. 자기 참조 테스트: 초대한 사람(Inviter) 정보가 잘 저장되어야 한다")
    void inviterRelationshipTest() {
        // given
        User inviter = User.builder()
                .loginId("inviter")
                .password("1234")
                .email("inviter@test.com")
                .filePermission(createKey())
                .build();
        userRepository.save(inviter);

        // when
        User invitee = User.builder()
                .loginId("invitee")
                .password("1234")
                .email("invitee@test.com")
                .filePermission(createKey())
                .inviter(inviter) // [핵심] 초대자 연결
                .build();
        userRepository.save(invitee);

        em.flush();
        em.clear();

        // then
        User foundInvitee = userRepository.findById(invitee.getId()).orElseThrow();

        assertThat(foundInvitee.getInviter()).isNotNull();
        assertThat(foundInvitee.getInviter().getId()).isEqualTo(inviter.getId());

        System.out.println("초대받은 사람: " + foundInvitee.getLoginId());
        System.out.println("초대한 사람: " + foundInvitee.getInviter().getLoginId());
    }
}