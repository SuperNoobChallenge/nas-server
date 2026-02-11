# Codex Session Handoff - NAS Server

Last Updated: 2026-02-11
Scope: `C:\GitFile\nas-server`
Audience: LLM/Codex only
Update Policy: This file can and should be updated multiple times during the project. Keep appending/revising session notes as work progresses.

## 1. Current Direction
- Core domain order settled around `FilePermissionKey` 중심 설계.
- 용량 부여/회수는 즉시 반영이 아니라 반드시 배치 큐를 거쳐 반영.
- User 생성 시 `UserPermission` 동반 생성이 필수.
- 시스템 감사 사용자 ID는 `1`로 고정.
- 로그인/로그아웃은 세션 기반으로 구현.

## 2. What Was Implemented

### 2.1 Capacity / Batch Flow
- Added repository:
  - `src/main/java/io/github/supernoobchallenge/nasserver/file/capacity/repository/CapacityAllocationRepository.java`
- Added request service (enqueue only):
  - `src/main/java/io/github/supernoobchallenge/nasserver/file/capacity/service/CapacityAllocationService.java`
  - Job type: `FILE_PERMISSION_CAPACITY_APPLY`
  - Job data key: `receiverPermissionId`, `giverPermissionId`, `amount`, `operation`(GRANT/REVOKE), `allocationType`, `description`
- Added batch handler (actual apply):
  - `src/main/java/io/github/supernoobchallenge/nasserver/batch/handler/impl/FilePermissionCapacityApplyHandler.java`
  - Locks with `findByIdForUpdate` and ordered lock by id to reduce deadlock chance.
- Extended `FilePermissionKey` domain logic:
  - `grantCapacity(long)`
  - `revokeCapacity(long)`
  - existing `adjustTotalCapacity(Long)` now delegates to grant/revoke.
- Extended `FilePermissionKeyRepository`:
  - `findByIdForUpdate(Long)` with `PESSIMISTIC_WRITE`.
- `BatchJobService.registerJob(...)` now sets defaults for batch fields.
- `BatchJobQueue.started_at` is nullable and is set only when worker picks a job.
- **Important bug fix** in batch status updates:
  - `BatchJobQueueRepository` `@Modifying` queries set `flushAutomatically=true` (+ existing `clearAutomatically=true`).
  - Reason: entity dirty state from handler was getting cleared before flush in worker flow.

### 2.2 User Domain / Security
- Added `PasswordEncoder` bean (`BCryptPasswordEncoder`) in:
  - `src/main/java/io/github/supernoobchallenge/nasserver/global/config/SecurityConfig.java`
- Added `UserService`:
  - `src/main/java/io/github/supernoobchallenge/nasserver/user/service/UserService.java`
  - `register(loginId, rawPassword, email, inviterId)`
  - `changePassword(userId, currentRawPassword, newRawPassword)`
- Registration behavior:
  - duplicate check: loginId/email
  - `FilePermissionKey` 생성 (`ownerType=USER`)
  - inviter 있으면 parentPermission = inviter.filePermission
  - password는 해시로 저장
  - `UserPermission`를 반드시 생성/연결 후 user 저장(단일 흐름)
- `UserRepository` additions:
  - `existsByLoginId`, `existsByEmail`
- Auditor system user id adjusted:
  - `src/main/java/io/github/supernoobchallenge/nasserver/global/security/AuditorAwareImpl.java`
  - `SYSTEM_USER_ID = 1L`
- Added auth API/service:
  - `src/main/java/io/github/supernoobchallenge/nasserver/user/service/AuthService.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/user/controller/AuthController.java`
  - `POST /api/auth/login`, `POST /api/auth/logout`
  - Session attributes: `LOGIN_USER_ID`, `LOGIN_ID`

## 3. Tests Added/Updated

### 3.1 Unit Tests
- `src/test/java/io/github/supernoobchallenge/nasserver/file/capacity/service/CapacityAllocationServiceTest.java`
- `src/test/java/io/github/supernoobchallenge/nasserver/batch/handler/impl/FilePermissionCapacityApplyHandlerTest.java`
- `src/test/java/io/github/supernoobchallenge/nasserver/user/service/UserServiceTest.java`
  - includes inviter-based registration test
- `src/test/java/io/github/supernoobchallenge/nasserver/user/service/AuthServiceTest.java`

### 3.2 Integration Tests
- `src/test/java/io/github/supernoobchallenge/nasserver/file/capacity/integration/CapacityAllocationBatchIntegrationTest.java`
  - enqueue -> handler apply -> capacity/allocation persisted
- `src/test/java/io/github/supernoobchallenge/nasserver/batch/scheduler/BatchJobWorkerCapacityIntegrationTest.java`
  - enqueue -> `BatchJobWorker.processPendingJobs()` -> job success + domain changes persisted
- `src/test/java/io/github/supernoobchallenge/nasserver/user/integration/UserServiceIntegrationTest.java`
  - Spring + real DB: user register/changePassword persistence verified
- `src/test/java/io/github/supernoobchallenge/nasserver/user/integration/AuthControllerIntegrationTest.java`
  - Spring + real DB: login/logout session flow verified

### 3.3 Existing Test Expectation Changed
- `src/test/java/io/github/supernoobchallenge/nasserver/repository/FilePermissionKeyRepositoryTest.java`
  - `createdBy/updatedBy` expectation updated from 0 to 1

## 4. Useful Verification Commands
- Capacity + user fast checks:
  - `./gradlew.bat test --tests "*CapacityAllocationServiceTest" --tests "*FilePermissionCapacityApplyHandlerTest" --tests "*CapacityAllocationBatchIntegrationTest" --tests "*BatchJobWorkerCapacityIntegrationTest" --tests "*UserServiceTest" --tests "*AuthServiceTest" --tests "*UserServiceIntegrationTest" --tests "*AuthControllerIntegrationTest"`
- Compile only:
  - `./gradlew.bat compileJava`

## 5. Known Constraints / Assumptions
- Tests currently run with local MySQL config (`db.properties`) in this environment.
- Audit fields are non-null and depend on `AuditorAwareImpl`.
- Batch job model currently uses string statuses/types, not enums.
- Integration tests prefer real DB (`@AutoConfigureTestDatabase(replace = NONE)`).

## 6. Suggested Next Steps
1. Add API layer for user register/password change.
2. Tighten security rules (separate public/authenticated endpoints instead of permit-all).
3. Add API layer for capacity request enqueue.
4. Implement email-link password reset flow.
5. Add admin recovery flow on boot as described in requirements.

## 7. Open Issues / TODO Priority
1. `SecurityConfig` is currently `permitAll` for every endpoint; authorization boundaries are not enforced yet.
2. User register/password-change controller endpoints are still missing (service exists, API not exposed).
3. Password reset flow is not requirement-complete:
   - currently only current-password change exists
   - email-link reset flow (token issuance/validation/expiry) is not implemented.
4. Admin recovery flow on boot is not implemented yet.
5. Batch model consistency cleanup pending:
   - status/jobType are raw strings (not enums)
   - naming drift exists between schema docs and entity field names (`retry_count` vs `attempt_count`, etc).
6. Potential model decision pending: whether `User.filePermission` should remain `@OneToOne` or be moved to `@ManyToOne` by policy.
