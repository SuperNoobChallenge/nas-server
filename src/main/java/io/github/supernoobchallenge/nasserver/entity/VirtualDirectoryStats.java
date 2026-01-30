package io.github.supernoobchallenge.nasserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "virtual_directory_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VirtualDirectoryStats extends AuditEntity{
    @Id
    @Column(name = "directory_id")
    private Long directoryId;

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
}
