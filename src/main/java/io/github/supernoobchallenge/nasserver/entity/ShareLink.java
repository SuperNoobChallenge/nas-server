package io.github.supernoobchallenge.nasserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "share_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShareLink extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "share_link_id")
    private Long shareLinkId;

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
}
