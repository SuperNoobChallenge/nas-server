package io.github.supernoobchallenge.nasserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPermission extends AuditEntity{
    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne
    @MapsId // users 테이블의 PK를 공유함
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private boolean canInvitePerson;

    @Column(nullable = false)
    private boolean canShareFile;

    @Column(nullable = false)
    private boolean canStorePersonal;

    @Column(nullable = false)
    private boolean canCreateGroup;

    @Builder
    public UserPermission(User user, boolean canInvitePerson, boolean canShareFile, boolean canStorePersonal, boolean canCreateGroup) {
        this.user = user;
        this.canInvitePerson = canInvitePerson;
        this.canShareFile = canShareFile;
        this.canStorePersonal = canStorePersonal;
        this.canCreateGroup = canCreateGroup;
    }
}
