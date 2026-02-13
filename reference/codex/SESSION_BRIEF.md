# SESSION_BRIEF

Updated: 2026-02-13
Project: nas-server
Path: C:\GitFile\nas-server
Purpose: Short, practical briefing for the next Codex session.

## A. Recently Completed

### 0) GitHub README created
- Added root `README.md` for repository onboarding.
- Includes:
  - project overview and tech stack
  - current implementation status summary
  - current code structure tree (`src/main`, `src/test`, `reference/codex`)
  - run/test commands
  - configuration notes and next priorities
- Key file:
  - README.md

### 1) Capacity allocation through batch only
- Capacity changes are requested via batch queue, not applied directly.
- Key files:
  - src/main/java/io/github/supernoobchallenge/nasserver/file/capacity/service/CapacityAllocationService.java
  - src/main/java/io/github/supernoobchallenge/nasserver/batch/handler/impl/FilePermissionCapacityApplyHandler.java
  - src/main/java/io/github/supernoobchallenge/nasserver/file/core/entity/FilePermissionKey.java

### 2) Batch queue stability fixes
- `started_at` now nullable in entity and only set when a job is actually picked by worker.
- Worker pickup query updates `startedAt = CURRENT_TIMESTAMP`.
- `@Modifying` queries in batch repository use `flushAutomatically=true` to avoid losing dirty changes before clear.
- Key files:
  - src/main/java/io/github/supernoobchallenge/nasserver/batch/entity/BatchJobQueue.java
  - src/main/java/io/github/supernoobchallenge/nasserver/batch/repository/BatchJobQueueRepository.java

### 3) User domain enhancements
- User registration + password change service implemented.
- Password is hashed with BCrypt.
- User creation now always includes `UserPermission` creation/attach.
- Inviter-based registration sets `parentPermission` from inviter's file permission.
- System auditor id is fixed to 1.
- Key files:
  - src/main/java/io/github/supernoobchallenge/nasserver/user/service/UserService.java
  - src/main/java/io/github/supernoobchallenge/nasserver/global/config/SecurityConfig.java
  - src/main/java/io/github/supernoobchallenge/nasserver/global/security/AuditorAwareImpl.java

### 4) Login/logout implemented (session-based)
- API:
  - POST /api/auth/login
  - POST /api/auth/logout
- Session keys:
  - LOGIN_USER_ID
  - LOGIN_ID
- Key files:
  - src/main/java/io/github/supernoobchallenge/nasserver/user/service/AuthService.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/controller/AuthController.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/dto/LoginRequest.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/dto/LoginResponse.java

### 5) User REST API added (register/change-password)
- Added REST endpoints:
  - POST /api/users/invite-register
  - PATCH /api/users/{userId}/password
- Added DTOs:
  - InviteRegisterRequest, InviteRegisterResponse, ChangePasswordRequest, ErrorResponse
- Added controller-level `IllegalArgumentException` -> HTTP 400 mapping.
- Key files:
  - src/main/java/io/github/supernoobchallenge/nasserver/user/controller/UserController.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/dto/InviteRegisterRequest.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/dto/InviteRegisterResponse.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/dto/ChangePasswordRequest.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/dto/ErrorResponse.java

### 6) Share invitation flow added (share link -> invited user register)
- Added invite link creation API:
  - POST /api/share-links/invites
- Added invite-based register API:
  - POST /api/users/invite-register
- Flow:
  - inviter creates share invite link (`shareUuid`, expiration, maxUseCount, optional link password)
  - invited user registers using `shareUuid` (+ link password if required)
  - inviter is connected to invited user (`users.inviter_id`)
  - share link `currentUseCount` increments
  - `UserService` signup API split:
    - direct signup: `register(loginId, rawPassword, email)`
    - invite signup: `registerInvitedUser(loginId, rawPassword, email, inviterId)`
  - Controller policy: direct public signup (`POST /api/users`) is disabled; invite-register only.
  - invite link 생성 시 inviter는 요청 바디가 아니라 로그인 세션 사용자 ID를 사용.
- Key files:
  - src/main/java/io/github/supernoobchallenge/nasserver/share/controller/ShareInvitationController.java
  - src/main/java/io/github/supernoobchallenge/nasserver/share/service/ShareInvitationService.java
  - src/main/java/io/github/supernoobchallenge/nasserver/share/repository/ShareLinkRepository.java
  - src/main/java/io/github/supernoobchallenge/nasserver/share/entity/ShareLink.java
  - src/main/java/io/github/supernoobchallenge/nasserver/share/dto/CreateInviteLinkRequest.java
  - src/main/java/io/github/supernoobchallenge/nasserver/share/dto/CreateInviteLinkResponse.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/dto/InviteRegisterRequest.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/dto/InviteRegisterResponse.java

### 7) Spring Security authorization boundaries applied
- Public endpoints:
  - POST /api/auth/login
  - POST /api/users/invite-register
  - POST /api/users/password-reset/request
  - POST /api/users/password-reset/confirm
- Auth required:
  - POST /api/share-links/invites
  - PATCH /api/users/{userId}/password
  - all remaining endpoints
- Login now stores Spring Security context in session.
- Key files:
  - src/main/java/io/github/supernoobchallenge/nasserver/global/config/SecurityConfig.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/service/AuthService.java

### 8) Email-link based password reset flow added
- Added public APIs:
  - POST /api/users/password-reset/request
  - POST /api/users/password-reset/confirm
- Flow:
  - email 입력으로 사용자 조회 후 비밀번호 재설정 토큰 링크 발급
  - 토큰은 `share_links`를 `PASSWORD_RESET` 타입으로 저장(만료 30분, 1회 사용)
  - 토큰 검증(삭제/타입/만료/사용횟수) 후 새 비밀번호 해시 저장
  - 재설정 성공 시 링크 사용 처리(`currentUseCount` 증가 + soft delete)
- 메일 발송은 `PasswordResetMailService` 인터페이스로 분리, 기본 구현은 로깅 방식
- Key files:
  - src/main/java/io/github/supernoobchallenge/nasserver/user/service/PasswordResetService.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/service/PasswordResetMailService.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/service/LoggingPasswordResetMailService.java
  - src/main/java/io/github/supernoobchallenge/nasserver/user/controller/UserController.java
  - src/main/java/io/github/supernoobchallenge/nasserver/share/entity/ShareLink.java
  - src/main/java/io/github/supernoobchallenge/nasserver/share/repository/ShareLinkRepository.java

### 9) Entity `@Id` field naming standardized to `id`
- Updated entity id field names to `id` where `@Id` was using domain-specific names.
- Updated JPQL property paths accordingly (`vf.id`, `vf.realFile.id`, `rf.id`).
- Key files:
  - src/main/java/io/github/supernoobchallenge/nasserver/file/core/entity/RealFile.java
  - src/main/java/io/github/supernoobchallenge/nasserver/file/core/entity/VirtualFile.java
  - src/main/java/io/github/supernoobchallenge/nasserver/file/core/entity/VirtualDirectoryStats.java
  - src/main/java/io/github/supernoobchallenge/nasserver/file/transfer/entity/UploadPart.java
  - src/main/java/io/github/supernoobchallenge/nasserver/file/transfer/entity/UploadSession.java
  - src/main/java/io/github/supernoobchallenge/nasserver/group/entity/Group.java
  - src/main/java/io/github/supernoobchallenge/nasserver/group/entity/GroupInvite.java
  - src/main/java/io/github/supernoobchallenge/nasserver/group/entity/GroupUser.java
  - src/main/java/io/github/supernoobchallenge/nasserver/group/entity/GroupUserPermission.java
  - src/main/java/io/github/supernoobchallenge/nasserver/share/entity/ShareLink.java
  - src/main/java/io/github/supernoobchallenge/nasserver/file/core/repository/RealFileRepository.java
  - src/main/java/io/github/supernoobchallenge/nasserver/file/core/repository/VirtualFileRepository.java
  - src/main/java/io/github/supernoobchallenge/nasserver/share/service/ShareInvitationService.java

### 10) Batch worker capacity integration test stabilization
- `BatchJobWorkerCapacityIntegrationTest` was made robust against shared-DB leftovers.
- Existing runnable jobs are excluded before the assertion flow, and newly created job/allocation are identified by baseline id + unique description.
- Key files:
  - src/test/java/io/github/supernoobchallenge/nasserver/batch/scheduler/BatchJobWorkerCapacityIntegrationTest.java

### 11) Batch worker runnable-job polling fix
- `BatchJobWorker` now queries only runnable jobs (`nextRunAt <= now`) instead of loading mixed wait/retry rows and filtering in-memory.
- This prevents starvation where low-id future `retry_wait` rows block newer runnable `wait` rows from being picked.
- Key files:
  - src/main/java/io/github/supernoobchallenge/nasserver/batch/scheduler/BatchJobWorker.java
  - src/main/java/io/github/supernoobchallenge/nasserver/batch/repository/BatchJobQueueRepository.java

### 12) Batch worker capacity integration test deterministic runnable-time setup
- `BatchJobWorkerCapacityIntegrationTest` now forces queued job `nextRunAt` to past time before worker run.
- This removes intermittent `status=wait` remains caused by boundary timing at `nextRunAt <= now`.
- Key files:
  - src/test/java/io/github/supernoobchallenge/nasserver/batch/scheduler/BatchJobWorkerCapacityIntegrationTest.java

### 13) Issue classification note (for next session)
- `BatchJobWorker` polling change (`nextRunAt <= now` in DB query) is a **root-cause fix** for runnable-job starvation.
- `BatchJobWorkerCapacityIntegrationTest` `nextRunAt` forced-past update is a **test determinism hardening** to remove timing-boundary flakiness.
- Key files:
  - src/main/java/io/github/supernoobchallenge/nasserver/batch/scheduler/BatchJobWorker.java
  - src/main/java/io/github/supernoobchallenge/nasserver/batch/repository/BatchJobQueueRepository.java
  - src/test/java/io/github/supernoobchallenge/nasserver/batch/scheduler/BatchJobWorkerCapacityIntegrationTest.java

## B. Test Status

### Unit tests
- Capacity service/handler unit tests: passing
- User service unit tests: passing
- Auth service unit tests: passing
- Password reset service unit tests: passing

### Integration tests (Spring + real DB)
- UserServiceIntegrationTest: passing
- AuthControllerIntegrationTest: passing
- UserControllerIntegrationTest: passing
- ShareInvitationIntegrationTest: passing
- CapacityAllocationBatchIntegrationTest: passing
- BatchJobWorkerCapacityIntegrationTest: passing

## C. Constraints / Environment Notes
- DB config comes from src/main/resources/db.properties.
- Tests are designed to run with real DB (`@AutoConfigureTestDatabase(replace = NONE)` where applicable).
- Audit columns are not-null; `AuditorAwareImpl` must return a valid system id (currently 1).

## D. Suggested Immediate Next Work
1. Add admin recovery on boot as per requirement docs.
2. Add API layer for capacity request enqueue.
3. Expand README API section with request/response examples for user/auth/share/password-reset APIs.

## D-1. Open Issues / TODO Priority
1. Invite link does not currently bind target email in DB (share_links schema has no invitee email column).
2. Password reset token is currently stored in `share_links` (`PASSWORD_RESET`) rather than dedicated token table.
3. Password reset mail service default implementation is logging only (real SMTP/provider adapter not wired yet).
4. Admin recovery-on-boot flow is still missing.
5. Batch naming consistency and enum refactor are pending.
6. `User.filePermission` cardinality policy (`@OneToOne` vs `@ManyToOne`) is undecided.

## E. Quick Verification Commands
- Compile:
  - .\gradlew.bat compileJava
- Auth + user integration tests:
  - .\gradlew.bat test --tests "*UserServiceIntegrationTest" --tests "*AuthControllerIntegrationTest"
- Capacity + worker integration tests:
  - .\gradlew.bat test --tests "*CapacityAllocationBatchIntegrationTest" --tests "*BatchJobWorkerCapacityIntegrationTest"
