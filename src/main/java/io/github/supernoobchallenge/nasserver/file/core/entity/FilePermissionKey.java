package io.github.supernoobchallenge.nasserver.file.core.entity;

import io.github.supernoobchallenge.nasserver.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
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
    private OwnerType ownerType;

    @Column(nullable = false)
    private Long totalCapacity;

    @Column(nullable = false)
    private Long availableCapacity;

    public enum OwnerType { USER, GROUP }

    @Builder
    public FilePermissionKey(FilePermissionKey parentPermission, OwnerType ownerType) {
        this.parentPermission = parentPermission;
        this.ownerType = ownerType;
        this.totalCapacity = 0L;
        this.availableCapacity = 0L;
    }

    /**
     * 배치 핸들러에서 사용하는 "용량 부여" 반영 메서드.
     * 총 용량/가용 용량을 동일하게 증가시킨다.
     */
    public void grantCapacity(long amount) {
        validatePositiveAmount(amount);
        this.totalCapacity += amount;
        this.availableCapacity += amount;
    }

    /**
     * 배치 핸들러에서 사용하는 "용량 회수" 반영 메서드.
     * 총 용량/가용 용량을 동일하게 감소시킨다.
     */
    public void revokeCapacity(long amount) {
        validatePositiveAmount(amount);
        long newTotal = this.totalCapacity - amount;
        long newAvailable = this.availableCapacity - amount;

        if (newTotal < 0) {
            throw new IllegalArgumentException("총 용량은 0보다 작을 수 없습니다.");
        }
        if (newAvailable < 0) {
            throw new IllegalArgumentException(
                    String.format("회수 가능한 가용 용량이 부족합니다. 현재: %d, 회수: %d", this.availableCapacity, amount)
            );
        }

        this.totalCapacity = newTotal;
        this.availableCapacity = newAvailable;
    }

    // ==========================================
    // 비즈니스 로직 메서드
    // ==========================================

    /**
     * 총 용량 변경 (관리자 부여, 이벤트 등)
     * 로직: 총 용량이 변하면, 그만큼 가용 용량도 똑같이 변해야 함 (사용량은 불변이므로)
     * 강제성을 지니는 이벤트이므로 사용 가능한 용량은 0 이하로 내려가도 상관없음
     */
    public void adjustTotalCapacity(Long amount) {
        if (amount == null || amount == 0L) {
            return;
        }

        if (amount > 0L) {
            grantCapacity(amount);
            return;
        }

        revokeCapacity(Math.abs(amount));
    }

    /**
     * 가용 용량 조정 (하위 유저에게 할당하거나 회수할 때)
     * @param amount 음수(할당해줌), 양수(회수함)
     */
    public void adjustAvailableCapacity(Long amount) {
        long newAvailable = this.availableCapacity + amount;

        // 1. 0 미만 체크 (더 이상 줄 게 없음)
        if (newAvailable < 0) {
            throw new IllegalArgumentException(
                    String.format("가용 용량이 부족합니다. 현재: %d, 요청: %d", this.availableCapacity, amount)
            );
        }

        // 2. Total 초과 체크 (총 용량보다 많이 가질 순 없음)
        if (newAvailable > this.totalCapacity) {
            throw new IllegalArgumentException(
                    String.format("가용 용량이 총 용량을 초과할 수 없습니다. Total: %d", this.totalCapacity)
            );
        }

        this.availableCapacity = newAvailable;
    }

    private void validatePositiveAmount(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("용량 변경값은 1 이상이어야 합니다.");
        }
    }
}
