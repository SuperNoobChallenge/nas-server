package io.github.supernoobchallenge.nasserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    // FK: 파일 권한 키 (1:1에 가깝지만 로직상 N:1로 매핑 가능, 여기선 OneToOne으로 가정)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_permission_id", nullable = false)
    private FilePermissionKey filePermission;

    // FK: 나를 초대한 유저 (Self Reference)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id")
    private User inviter;

    // 1:1 관계 매핑
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserPermission userPermission;

    @Builder
    public User(String loginId, String password, String email, FilePermissionKey filePermission, User inviter) {
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.filePermission = filePermission;
        this.inviter = inviter;
    }

    // ==========================================
    // 비즈니스 로직 메서드
    // ==========================================

    /**
     * 비밀번호 변경
     * @param newPassword 암호화된 새 비밀번호
     */
    public void changePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호는 비어있을 수 없습니다.");
        }

        if (this.password.equals(newPassword)) {
            throw new IllegalArgumentException("기존 비밀번호와 동일합니다.");
        }

        this.password = newPassword;
    }

    /**
     * 이메일 변경
     * @param newEmail 새 이메일 주소
     */
    public void updateEmail(String newEmail) {
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("이메일 주소는 비어있을 수 없습니다.");
        }

        this.email = newEmail;
    }

    // (참고) 기존 setPermission도 이름 변경 추천
    public void assignPermission(UserPermission userPermission) {
        this.userPermission = userPermission;
    }
}
