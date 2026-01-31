package io.github.supernoobchallenge.nasserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
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

    // ==========================================
    // 1. Builder 생성자
    // ==========================================
    @Builder
    public VirtualDirectory(FilePermissionKey filePermission, VirtualDirectory parentDirectory, int readLevel, int writeLevel, String name) {
        this.filePermission = filePermission;
        this.parentDirectory = parentDirectory;
        this.readLevel = readLevel;
        this.writeLevel = writeLevel;
        this.name = name;

        // depthLevel은 부모에 따라 자동으로 결정됨 (생성 시점)
        // 부모가 없으면(Root) 0, 있으면 부모+1
        this.depthLevel = (parentDirectory == null) ? 0 : parentDirectory.getDepthLevel() + 1;
    }

    // ==========================================
    // 2. 비즈니스 로직 메서드
    // ==========================================

    /**
     * 디렉터리 이름 변경 (Rename)
     */
    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("디렉터리 이름은 비어있을 수 없습니다.");
        }

        this.name = newName;
    }

    /**
     * 권한 레벨 변경 (Change Permissions)
     * @param readLevel 읽기 권한 레벨
     * @param writeLevel 쓰기 권한 레벨
     */
    public void updateAccessLevel(int readLevel, int writeLevel) {
        if (readLevel < 0 || writeLevel < 0) {
            throw new IllegalArgumentException("권한 레벨은 0 이상이어야 합니다.");
        }

        if (readLevel > writeLevel){
            throw new IllegalArgumentException("읽기 레벨은 쓰기 레벨보다 작거나 같아야 합니다.");
        }
        // (선택) 읽기 레벨이 쓰기 레벨보다 높아야 한다 등의 정책이 있다면 검증 추가
        this.readLevel = readLevel;
        this.writeLevel = writeLevel;
    }

    /**
     * 디렉터리 이동 (Move)
     * 부모 디렉터리가 변경되면 DepthLevel도 자동으로 재계산됩니다.
     * @param newParent 이동할 목적지 디렉터리 (null이면 Root로 이동)
     */
    public void moveDirectory(VirtualDirectory newParent) {
        // 1. 자기 자신 밑으로 이동 방지 (기본적인 순환 참조 체크)
        if (newParent != null && newParent.getId().equals(this.id)) {
            throw new IllegalArgumentException("자기 자신을 부모로 설정할 수 없습니다.");
        }

        // 2. 부모 변경
        this.parentDirectory = newParent;

        // 3. 깊이 재계산 (부모가 null이면 0, 아니면 부모 + 1)
        this.depthLevel = (newParent == null) ? 0 : newParent.getDepthLevel() + 1;
    }

    // 통계 테이블 초기화용 편의 메서드
    public void initStat(VirtualDirectoryStats stat) {
        this.stat = stat;
    }
}
