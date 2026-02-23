package io.github.supernoobchallenge.nasserver.file.core.entity;

import io.github.supernoobchallenge.nasserver.global.entity.AuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "virtual_directory_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VirtualDirectoryStats extends AuditEntity {
    @Id
    @Column(name = "directory_id")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "directory_id")
    private VirtualDirectory virtualDirectory;

    @Column(nullable = false)
    private Long totalSize = 0L;

    @Column(nullable = false)
    private int directoryCount = 0;

    @Column(nullable = false)
    private int fileCount = 0;

    private VirtualDirectoryStats(VirtualDirectory virtualDirectory) {
        this.virtualDirectory = virtualDirectory;
    }

    public static VirtualDirectoryStats init(VirtualDirectory virtualDirectory) {
        if (virtualDirectory == null) {
            throw new IllegalArgumentException("virtualDirectory는 필수입니다.");
        }
        return new VirtualDirectoryStats(virtualDirectory);
    }
}
