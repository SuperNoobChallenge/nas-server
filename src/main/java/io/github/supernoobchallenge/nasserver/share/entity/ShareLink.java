package io.github.supernoobchallenge.nasserver.share.entity;

import io.github.supernoobchallenge.nasserver.global.entity.BaseEntity;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "share_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShareLink extends BaseEntity {
    public static final String LINK_TYPE_INVITE = "INVITE";
    public static final String LINK_TYPE_PASSWORD_RESET = "PASSWORD_RESET";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "share_link_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 32)
    private String shareUuid;

    @Column(nullable = false, length = 15)
    private String linkType; // INVITE, FILE, GROUP

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    private String password;

    @Column(nullable = false)
    private int maxUseCount;

    @Column(nullable = false)
    private int currentUseCount;

    @Column(nullable = false, length = 50)
    private String name;

    @Builder
    private ShareLink(User user, String shareUuid, String linkType, LocalDateTime expirationDate,
                      String password, int maxUseCount, int currentUseCount, String name) {
        this.user = user;
        this.shareUuid = shareUuid;
        this.linkType = linkType;
        this.expirationDate = expirationDate;
        this.password = password;
        this.maxUseCount = maxUseCount;
        this.currentUseCount = currentUseCount;
        this.name = name;
    }

    public static ShareLink createInviteLink(User inviter, String shareUuid, LocalDateTime expirationDate,
                                             String encodedPassword, int maxUseCount, String name) {
        if (inviter == null) {
            throw new IllegalArgumentException("inviter는 필수입니다.");
        }
        if (shareUuid == null || shareUuid.isBlank()) {
            throw new IllegalArgumentException("shareUuid는 필수입니다.");
        }
        if (expirationDate == null) {
            throw new IllegalArgumentException("expirationDate는 필수입니다.");
        }
        if (maxUseCount < 1) {
            throw new IllegalArgumentException("maxUseCount는 1 이상이어야 합니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 비어있을 수 없습니다.");
        }

        return ShareLink.builder()
                .user(inviter)
                .shareUuid(shareUuid)
                .linkType(LINK_TYPE_INVITE)
                .expirationDate(expirationDate)
                .password(encodedPassword)
                .maxUseCount(maxUseCount)
                .currentUseCount(0)
                .name(name)
                .build();
    }

    public static ShareLink createPasswordResetLink(User user, String resetToken, LocalDateTime expirationDate) {
        if (user == null) {
            throw new IllegalArgumentException("user는 필수입니다.");
        }
        if (resetToken == null || resetToken.isBlank()) {
            throw new IllegalArgumentException("resetToken은 필수입니다.");
        }
        if (expirationDate == null) {
            throw new IllegalArgumentException("expirationDate는 필수입니다.");
        }

        return ShareLink.builder()
                .user(user)
                .shareUuid(resetToken)
                .linkType(LINK_TYPE_PASSWORD_RESET)
                .expirationDate(expirationDate)
                .password(null)
                .maxUseCount(1)
                .currentUseCount(0)
                .name("password-reset")
                .build();
    }

    public boolean isInviteLink() {
        return LINK_TYPE_INVITE.equals(this.linkType);
    }

    public boolean isPasswordResetLink() {
        return LINK_TYPE_PASSWORD_RESET.equals(this.linkType);
    }

    public boolean hasPassword() {
        return this.password != null && !this.password.isBlank();
    }

    public boolean isExpired(LocalDateTime now) {
        return expirationDate.isBefore(now);
    }

    public boolean isExhausted() {
        return currentUseCount >= maxUseCount;
    }

    public void increaseUseCount() {
        if (isExhausted()) {
            throw new IllegalArgumentException("사용 가능 횟수를 초과했습니다.");
        }
        this.currentUseCount += 1;
    }
}
