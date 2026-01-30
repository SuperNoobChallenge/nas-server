package io.github.supernoobchallenge.nasserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "share_directories",
        indexes = @Index(name = "idx_share_directories_link_parent", columnList = "share_link_id, deleted_at, parent_directory_id, name, share_directory_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShareDirectory extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "share_directory_id")
    private Long id; // 자바 필드는 id, DB 컬럼은 share_directory_id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_link_id", nullable = false)
    private ShareLink shareLink;

    // 부모 디렉터리 (Root인 경우 NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_directory_id")
    private ShareDirectory parentDirectory;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int depthLevel;
}
