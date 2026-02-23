# file Domain Package

파일 권한/용량, 가상 디렉터리/파일 구조, 실제 파일 참조 카운트, 업로드 세션 모델을 담당하는 도메인입니다.

## 구성 요소 역할
- `capacity/service/CapacityAllocationService`
  - 용량 부여/회수 요청을 검증하고 배치 작업으로 등록합니다.
- `capacity/entity/CapacityAllocation`
  - 용량 이동 이력(수신자/제공자/크기/타입/설명)을 저장합니다.
- `core/controller/VirtualDirectoryController`
  - 가상 디렉터리 생성/조회/이름변경/이동/삭제요청 API를 제공합니다.
- `core/service/VirtualDirectoryService`
  - 디렉터리 접근권한/중복이름/순환이동 검증과 배치 삭제 위임을 수행합니다.
- `core/entity/FilePermissionKey`
  - 총 용량/가용 용량 변경 규칙을 보장합니다.
- `core/repository/*`
  - 디렉터리 활성 조회, 형제 이름 중복 검사, 삭제 작업용 집계/soft delete/참조 카운트 감소를 담당합니다.
- `transfer/entity/*`
  - 업로드 세션/파트 저장 모델입니다(현재는 엔티티 중심).

## 주요 메서드 상호작용

### 1) 용량 부여/회수 요청 등록 흐름 (동기 검증 + 비동기 실행)
1. 상위 레이어가 `CapacityAllocationService.requestGrantCapacity(...)` 또는 `requestRevokeCapacity(...)` 호출
2. `validateRequest`로 필수값/양수 크기 검증
3. `FilePermissionKeyRepository.findById`로 receiver/giver 존재 검증
4. `jobData`(`receiverPermissionId`, `giverPermissionId`, `amount`, `operation` 등) 생성
5. `BatchJobService.registerJob(CapacityAllocationService.JOB_TYPE_CAPACITY_APPLY, ...)` 호출

핵심: `file` 도메인은 즉시 반영하지 않고 `batch` 도메인으로 위임합니다.

### 2) 용량 반영 실행 흐름 (배치 핸들러와의 협업)
1. `BatchJobWorker`가 `FILE_PERMISSION_CAPACITY_APPLY` 작업을 가져옴
2. `FilePermissionCapacityApplyHandler.handle(...)` 실행
3. `FilePermissionKeyRepository.findByIdForUpdate`로 receiver/giver 잠금 획득
4. `FilePermissionKey` 메서드 호출
   - 부여: `grantCapacity(amount)` + 필요 시 giver의 `adjustAvailableCapacity(-amount)`
   - 회수: `revokeCapacity(amount)` + 필요 시 giver의 `adjustAvailableCapacity(amount)`
5. `CapacityAllocationRepository.saveAll`로 이력 저장

핵심: 용량 수치의 무결성은 `FilePermissionKey` 도메인 메서드가 강제합니다.

### 3) 가상 디렉터리 생성/이동/삭제요청 흐름
1. `VirtualDirectoryController`가 세션 사용자 ID를 추출해 `VirtualDirectoryService` 호출
2. `VirtualDirectoryService`가 사용자 `filePermission` 기준 접근권한을 검증
3. 생성/이름변경/이동 시 `VirtualDirectoryRepository.existsActiveSiblingName`로 중복 이름 차단
4. 이동 시 `VirtualDirectoryRepository.findAllDescendantIds`로 하위 트리 순환 이동 차단
5. 삭제는 즉시 삭제하지 않고 `batch/DirectoryService.deleteDirectory`로 배치 작업 등록

핵심: 디렉터리 변경은 동기 검증으로 무결성을 지키고, 무거운 삭제는 비동기 배치로 위임합니다.

### 4) 파일/디렉터리 삭제 시 참조 카운트 연계 흐름
1. 배치 삭제 핸들러가 `VirtualFileRepository.countActiveByDirectoryIds` 또는 `countActiveByRealFileIds`로 감소량 집계
2. `VirtualFileRepository.softDeleteByDirectoryIds` 또는 `softDeleteByVirtualFileIds` 실행
3. 집계 결과를 기준으로 `RealFileRepository.decrementReferenceCount` 호출

핵심: 가상 파일 삭제와 실제 파일 참조 카운트 감소가 같이 움직입니다.

## 엔티티 메서드/정책 포인트
- `FilePermissionKey.grantCapacity`/`revokeCapacity`
  - 총 용량과 가용 용량을 함께 변경하고 음수/초과 상태를 차단합니다.
- `FilePermissionKey.adjustAvailableCapacity`
  - `0 <= available <= total` 제약을 강제합니다.
- `VirtualDirectory.rename`, `updateAccessLevel`, `moveDirectory`
  - 디렉터리 이름/권한/부모 이동 시 기본 검증과 depth 재계산을 수행합니다.

## 현재 상태 메모
- 가상 디렉터리 API(`/api/directories`)는 생성/조회/이름변경/이동/삭제요청까지 구현 완료되었습니다.
- `transfer` 패키지는 현재 업로드 세션/파트 엔티티만 존재하며, 서비스/컨트롤러 흐름은 아직 구현 전입니다.
