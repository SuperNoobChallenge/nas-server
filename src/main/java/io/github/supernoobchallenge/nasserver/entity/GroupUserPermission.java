package io.github.supernoobchallenge.nasserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group_user_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupUserPermission extends AuditEntity{
    @Id
    @Column(name = "group_user_id")
    private Long groupUserId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "group_user_id")
    private GroupUser groupUser;

    @Column(nullable = false)
    private boolean canManageUser;

    @Column(nullable = false)
    private boolean canManageFile;
}
