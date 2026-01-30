package io.github.supernoobchallenge.nasserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "virtual_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VirtualFile extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "virtual_file_id")
    private Long virtualFileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "directory_id", nullable = false)
    private VirtualDirectory directory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "real_file_id", nullable = false)
    private RealFile realFile;

    @Column(nullable = false)
    private int readLevel;

    @Column(nullable = false)
    private int writeLevel;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String extension;
}
