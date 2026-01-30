package io.github.supernoobchallenge.nasserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "virtual_directories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VirtualDirectory extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "directory_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_permission_id", nullable = false)
    private FilePermissionKey filePermission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_directory_id")
    private VirtualDirectory parentDirectory;

    @Column(nullable = false)
    private int readLevel;

    @Column(nullable = false)
    private int writeLevel;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int depthLevel;

    // 1:1 통계 테이블
    @OneToOne(mappedBy = "virtualDirectory", cascade = CascadeType.ALL)
    private VirtualDirectoryStats stat;
}
