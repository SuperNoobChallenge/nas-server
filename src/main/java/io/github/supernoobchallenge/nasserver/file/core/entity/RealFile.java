package io.github.supernoobchallenge.nasserver.file.core.entity;

import io.github.supernoobchallenge.nasserver.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "real_files", indexes = @Index(name = "idx_real_files_hash_size", columnList = "fileHash, fileSize"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RealFile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "real_file_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String storageUuid;

    @Column(nullable = false, length = 64)
    private String fileHash; // SHA-256

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String storagePath;

    @Column(nullable = false, length = 10)
    private String storageType; // LOCAL, S3

    @Column(nullable = false)
    private int referenceCount = 0;
}
