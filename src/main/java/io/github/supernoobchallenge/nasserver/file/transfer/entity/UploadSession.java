package io.github.supernoobchallenge.nasserver.file.transfer.entity;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.global.entity.AuditEntity;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "upload_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadSession extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_permission_id", nullable = false)
    private FilePermissionKey filePermission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 32)
    private String sessionUuid;

    @Column(nullable = false)
    private int totalParts;

    @Column(nullable = false)
    private Long totalSize;

    private String uploadTaskId; // Object Storage ID

    private String localPath;

    @Column(nullable = false)
    private LocalDateTime lastActiveAt;
}
