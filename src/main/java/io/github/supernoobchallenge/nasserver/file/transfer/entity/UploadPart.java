package io.github.supernoobchallenge.nasserver.file.transfer.entity;

import io.github.supernoobchallenge.nasserver.global.entity.AuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "upload_parts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadPart extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "part_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private UploadSession session;

    @Column(nullable = false)
    private int partNumber;

    @Column(nullable = false)
    private int partSize;

    @Column(length = 100)
    private String etag;
}
