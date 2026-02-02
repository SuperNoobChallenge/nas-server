package io.github.supernoobchallenge.nasserver.file.capacity.entity;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "capacity_allocations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CapacityAllocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granter_allocation_id")
    private CapacityAllocation granterAllocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_permission_id", nullable = false)
    private FilePermissionKey receiverPermission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "giver_permission_id")
    private FilePermissionKey giverPermission; // Nullable (System)

    @Column(nullable = false)
    private Long allocatedSize;

    private LocalDateTime expirationDate;

    @Column(nullable = false, length = 20)
    private String allocationType; // EVENT, GRANT, SYSTEM

    @Column(length = 100)
    private String description;
}
