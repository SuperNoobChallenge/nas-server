# Codex Session Handoff - NAS Server

Last Updated: 2026-02-13
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

### 2.0 Repository Documentation
- Added root GitHub README:
  - `README.md`
- README currently documents:
  - project purpose + stack
  - implementation status snapshot
  - current package/folder structure
  - run/test commands
  - configuration notes and near-term priorities

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
  - `register(loginId, rawPassword, email)` (direct register)
  - `registerInvitedUser(loginId, rawPassword, email, inviterId)` (invite register)
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
- Security authorization policy applied:
  - public: `POST /api/auth/login`, `POST /api/users/invite-register`, `POST /api/users/password-reset/request`, `POST /api/users/password-reset/confirm`
  - authenticated: all remaining endpoints
  - `AuthService.login(...)` stores Spring Security context in HTTP session.

### 2.3 User REST API (register/change-password)
- Added user controller:
  - `src/main/java/io/github/supernoobchallenge/nasserver/user/controller/UserController.java`
- Added endpoints:
  - `POST /api/users/invite-register` (invite register only)
  - `PATCH /api/users/{userId}/password` (change password)
- Added DTOs:
  - `src/main/java/io/github/supernoobchallenge/nasserver/user/dto/RegisterUserRequest.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/user/dto/RegisterUserResponse.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/user/dto/ChangePasswordRequest.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/user/dto/ErrorResponse.java`
- `IllegalArgumentException` from this controller is mapped to HTTP 400 with `{ "message": ... }`.
- Direct public signup endpoint `POST /api/users` is disabled by policy.

### 2.4 Share invitation link flow (based on share_links schema)
- Added share invitation controller/service:
  - `src/main/java/io/github/supernoobchallenge/nasserver/share/controller/ShareInvitationController.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/share/service/ShareInvitationService.java`
- Added share link repository:
  - `src/main/java/io/github/supernoobchallenge/nasserver/share/repository/ShareLinkRepository.java`
- Extended `ShareLink` entity with invite helper logic:
  - `createInviteLink(...)`, expiry/use-count checks, use-count increase.
- Added APIs:
  - `POST /api/share-links/invites` (create invitation link)
  - `POST /api/users/invite-register` (register via shareUuid, optional link password)
- Invite link 생성 시 inviter 식별자는 요청 DTO에서 받지 않고, 로그인 세션 기반 auditor(`getAuthenticatedAuditor`)로 결정.
- Invite register behavior:
  - validates link type/expiry/use-count/deleted state/password
  - creates user through `UserService.registerInvitedUser(...)`
  - sets inviter from share link owner
  - increments `share_links.current_use_count`

### 2.5 Email-link password reset flow
- Added password reset service and mail sender abstraction:
  - `src/main/java/io/github/supernoobchallenge/nasserver/user/service/PasswordResetService.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/user/service/PasswordResetMailService.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/user/service/LoggingPasswordResetMailService.java`
- Added public APIs:
  - `POST /api/users/password-reset/request`
  - `POST /api/users/password-reset/confirm`
- Implementation notes:
  - reset token is stored in `share_links` with `link_type=PASSWORD_RESET`
  - token TTL is 30 minutes and max use is 1
  - existing active reset links for user are invalidated when issuing a new one
  - confirm flow validates token state(type/expiry/use/deleted), updates password hash, and consumes link
  - default mail sender logs the reset URL (adapter point for real SMTP/provider)

### 2.6 Entity `@Id` field naming consistency (`id`)
- Standardized `@Id` field variable name to `id` across entities that previously used domain-specific id variable names.
- Updated JPQL property paths impacted by entity field rename:
  - `RealFileRepository`: `rf.realFileId` -> `rf.id`
  - `VirtualFileRepository`: `vf.virtualFileId` -> `vf.id`, `vf.realFile.realFileId` -> `vf.realFile.id`
- Updated invite response mapping to use `ShareLink.getId()`.
- Key files:
  - `src/main/java/io/github/supernoobchallenge/nasserver/file/core/entity/RealFile.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/file/core/entity/VirtualFile.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/file/core/entity/VirtualDirectoryStats.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/file/transfer/entity/UploadPart.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/file/transfer/entity/UploadSession.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/group/entity/Group.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/group/entity/GroupInvite.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/group/entity/GroupUser.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/group/entity/GroupUserPermission.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/share/entity/ShareLink.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/file/core/repository/RealFileRepository.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/file/core/repository/VirtualFileRepository.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/share/service/ShareInvitationService.java`

### 2.7 Batch worker capacity integration test hardening
- Stabilized `BatchJobWorkerCapacityIntegrationTest` for shared real-DB environments.
- Test now:
  - excludes pre-existing runnable jobs before running worker
  - uses baseline id snapshots for `batch_job_queues` and `capacity_allocations`
  - tracks created allocation by unique description token
- Key file:
  - `src/test/java/io/github/supernoobchallenge/nasserver/batch/scheduler/BatchJobWorkerCapacityIntegrationTest.java`

### 2.8 Batch worker polling starvation fix
- Root cause:
  - worker previously loaded `Top200` rows by status (`wait`, `retry_wait`) and filtered runnable state in-memory.
  - if low-id `retry_wait` rows were not yet due, runnable `wait` rows with higher ids could be skipped.
- Fix:
  - worker now queries only runnable rows at repository layer (`status in (...) AND nextRunAt <= now`) before limiting.
- Key files:
  - `src/main/java/io/github/supernoobchallenge/nasserver/batch/scheduler/BatchJobWorker.java`
  - `src/main/java/io/github/supernoobchallenge/nasserver/batch/repository/BatchJobQueueRepository.java`

### 2.9 BatchJobWorkerCapacityIntegrationTest timing-boundary fix
- Symptom:
  - assertion expected `success` but got `wait` on processed job status.
- Cause:
  - queued job could miss worker pick-up when `nextRunAt` and worker `now` are on a narrow boundary.
- Fix:
  - test explicitly updates queued job `nextRunAt` to `now - 5s` before calling `processPendingJobs()`.
- Key file:
  - `src/test/java/io/github/supernoobchallenge/nasserver/batch/scheduler/BatchJobWorkerCapacityIntegrationTest.java`

### 2.10 Root-cause fix vs test hardening (explicit)
- Root-cause fix:
  - worker polling query changed to fetch only runnable jobs at DB level (`nextRunAt <= now`).
- Test hardening:
  - integration test forces target job runnable-time to avoid boundary-time nondeterminism.
- Guidance:
  - keep both for now; do not remove test hardening unless worker execution timing is fully controlled in test environment.

## 3. Tests Added/Updated

### 3.1 Unit Tests
- `src/test/java/io/github/supernoobchallenge/nasserver/file/capacity/service/CapacityAllocationServiceTest.java`
- `src/test/java/io/github/supernoobchallenge/nasserver/batch/handler/impl/FilePermissionCapacityApplyHandlerTest.java`
- `src/test/java/io/github/supernoobchallenge/nasserver/user/service/UserServiceTest.java`
  - includes inviter-based registration test
- `src/test/java/io/github/supernoobchallenge/nasserver/user/service/AuthServiceTest.java`
- `src/test/java/io/github/supernoobchallenge/nasserver/user/service/PasswordResetServiceTest.java`

### 3.2 Integration Tests
- `src/test/java/io/github/supernoobchallenge/nasserver/file/capacity/integration/CapacityAllocationBatchIntegrationTest.java`
  - enqueue -> handler apply -> capacity/allocation persisted
- `src/test/java/io/github/supernoobchallenge/nasserver/batch/scheduler/BatchJobWorkerCapacityIntegrationTest.java`
  - enqueue -> `BatchJobWorker.processPendingJobs()` -> job success + domain changes persisted
- `src/test/java/io/github/supernoobchallenge/nasserver/user/integration/UserServiceIntegrationTest.java`
  - Spring + real DB: user register/changePassword persistence verified
- `src/test/java/io/github/supernoobchallenge/nasserver/user/integration/AuthControllerIntegrationTest.java`
  - Spring + real DB: login/logout session flow verified
- `src/test/java/io/github/supernoobchallenge/nasserver/user/integration/UserControllerIntegrationTest.java`
  - Spring + real DB: invite-register/password-change/password-reset + auth-boundary behavior verified
- `src/test/java/io/github/supernoobchallenge/nasserver/share/integration/ShareInvitationIntegrationTest.java`
  - Spring + real DB: invite link create + invite register + use-count guard verified

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
1. Add API layer for capacity request enqueue.
2. Add admin recovery flow on boot as described in requirements.
3. Update `README.md` with endpoint-level examples for user/auth/share-invite/password-reset APIs.
4. Consider dedicated password-reset token table and production mail adapter wiring.

## 7. Open Issues / TODO Priority
1. Invite-link model is not email-bound yet (schema `share_links` has no invitee email column):
   - current flow validates token/password/expiry/use-count only
   - if strict email-bound invite is required, schema/API extension is needed.
2. Password reset token currently reuses `share_links` table (`link_type=PASSWORD_RESET`) instead of a dedicated token table.
3. Password reset mail service default implementation is logging only (real SMTP/provider adapter pending).
4. Admin recovery flow on boot is not implemented yet.
5. Batch model consistency cleanup pending:
   - status/jobType are raw strings (not enums)
   - naming drift exists between schema docs and entity field names (`retry_count` vs `attempt_count`, etc).
6. Potential model decision pending: whether `User.filePermission` should remain `@OneToOne` or be moved to `@ManyToOne` by policy.
