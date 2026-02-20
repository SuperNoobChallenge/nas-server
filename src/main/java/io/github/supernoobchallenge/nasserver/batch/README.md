# batch Domain Package

배치 큐(`batch_job_queues`)를 통해 무거운 작업을 비동기로 처리하는 도메인입니다.

## 구성 요소 역할
- `config/BatchSchedulerConfig`
  - `@EnableScheduling`으로 워커 스케줄링을 활성화합니다.
- `service/BatchJobService`
  - 다른 도메인에서 받은 작업 요청을 `BatchJobQueue`로 저장합니다.
- `service/DirectoryService`
  - 디렉터리 삭제 요청을 즉시 삭제가 아니라 배치 작업 등록으로 전환합니다.
- `scheduler/BatchJobWorker`
  - 실행 가능한 작업만 폴링하고, 상태를 잠금 전이한 뒤, 타입별 핸들러로 라우팅합니다.
- `handler/BatchJobHandler` + `handler/impl/*`
  - `jobType`별 실제 비즈니스 로직을 수행합니다.
- `repository/BatchJobQueueRepository`
  - 폴링 조회, 상태 전이, 성공/실패/재시도 업데이트를 일괄 쿼리로 처리합니다.
- `entity/BatchJobQueue`
  - 작업 상태(`wait`, `in_progress`, `retry_wait`, `success`, `fail`)와 시도 횟수, 실행 시각을 보관합니다.

## 주요 메서드 상호작용

### 1) 디렉터리 삭제 요청이 배치로 전환되는 흐름
1. `DirectoryService.deleteDirectory(directoryId, userId)` 호출
2. `VirtualDirectoryRepository.findById`로 대상 존재 검증
3. `BatchJobService.registerJob("DIRECTORY_DELETE", "virtual_directories", ...)` 호출
4. `BatchJobQueue`가 `status=wait`, `attemptCount=0`, `nextRunAt=now`로 생성되어 저장

핵심: 요청 API/서비스는 즉시 삭제하지 않고 큐에 적재만 수행합니다.

### 2) 워커의 공통 처리 흐름
1. `BatchJobWorker.processPendingJobs()`가 1초 주기로 실행
2. `BatchJobQueueRepository.findTop200ByStatusInAndNextRunAtLessThanEqualOrderByIdAsc(...)`로 runnable 작업 조회
3. `updateStatusToProcessing(ids, "in_progress")`로 선점(중복 실행 방지 + attempt 증가 + startedAt 기록)
4. `findByIdInAndStatus(..., "in_progress")` 재조회 후 `jobType`별 그룹핑
5. 각 그룹에 대해 `handlerMap.get(jobType).handle(jobList)` 호출
6. 성공 시 `markAsSuccess(ids)`, 실패 시 재시도 가능 여부를 나눠 `markAsRetryWait` 또는 `markAsFail`

핵심: 상태 전이는 저장소 일괄 쿼리로 처리되고, 도메인 작업은 핸들러에 위임됩니다.

### 3) 핸들러별 실제 작업 흐름
- `VirtualDirectoryDeleteHandler.handle`
  1. `targetTable` 검증
  2. `targetId` 목록 추출
  3. `VirtualDirectoryRepository.findAllDescendantIds`로 하위 디렉터리 포함 대상 확장
  4. `VirtualFileRepository.countActiveByDirectoryIds`로 실제 파일 참조 감소량 계산
  5. `VirtualFileRepository.softDeleteByDirectoryIds` 실행
  6. `RealFileRepository.decrementReferenceCount` 반복 호출
  7. `VirtualDirectoryRepository.softDeleteInBatch` 실행
- `VirtualFileDeleteHandler.handle`
  1. `targetTable` 검증
  2. 파일 ID 추출
  3. `countActiveByRealFileIds`로 참조 감소량 계산
  4. `softDeleteByVirtualFileIds` 후 `decrementReferenceCount` 반영
- `FilePermissionCapacityApplyHandler.handle`
  1. `jobData` 파싱(`receiverPermissionId`, `giverPermissionId`, `amount`, `operation`)
  2. `lockPair`에서 `FilePermissionKeyRepository.findByIdForUpdate`로 잠금 획득
  3. `GRANT`: `giver.adjustAvailableCapacity(-amount)` 후 `receiver.grantCapacity(amount)`
  4. `REVOKE`: `receiver.revokeCapacity(amount)` 후 `giver.adjustAvailableCapacity(amount)`
  5. 결과 이력을 `CapacityAllocationRepository.saveAll`로 저장

## 실패/재시도 포인트
- 핸들러 미등록 `jobType`이면 즉시 `fail`
- 핸들러 예외 발생 시 `attemptCount`와 `maxAttempts` 기반으로 `retry_wait` 또는 `fail` 결정
- 재시도 시 `BatchJobWorker.getBackoff()` 정책으로 `nextRunAt` 계산
