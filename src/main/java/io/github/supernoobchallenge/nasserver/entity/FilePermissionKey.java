package io.github.supernoobchallenge.nasserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_permission_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FilePermissionKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_permission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_permission_id")
    private FilePermissionKey parentPermission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OwnerType ownerType; // ENUM: USER, GROUP

    @Column(nullable = false)
    private Long totalCapacity;

    @Column(nullable = false)
    private Long availableCapacity;

    public enum OwnerType { USER, GROUP }
}
