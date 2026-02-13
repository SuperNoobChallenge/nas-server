package io.github.supernoobchallenge.nasserver.group.entity;

import io.github.supernoobchallenge.nasserver.share.entity.ShareLink;
import io.github.supernoobchallenge.nasserver.global.entity.AuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group_invites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInvite extends AuditEntity {
    @Id
    @Column(name = "share_link_id")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "share_link_id")
    private ShareLink shareLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
}
