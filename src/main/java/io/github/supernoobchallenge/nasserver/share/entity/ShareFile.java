package io.github.supernoobchallenge.nasserver.share.entity;

import io.github.supernoobchallenge.nasserver.file.core.entity.VirtualFile;
import io.github.supernoobchallenge.nasserver.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "share_files",
        indexes = @Index(name = "idx_share_files_directory_name", columnList = "share_directory_id, deleted_at, name, share_file_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShareFile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "share_file_id")
    private Long id; // 자바 필드는 id, DB 컬럼은 share_file_id

    // 소속된 공유 디렉터리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_directory_id", nullable = false)
    private ShareDirectory shareDirectory;

    // 원본 가상 파일 참조 (읽기 전용 목적)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "virtual_file_id", nullable = false)
    private VirtualFile virtualFile;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String extension;
}
