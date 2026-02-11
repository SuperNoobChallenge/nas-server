# SESSION_BRIEF

Updated: 2026-02-11
Project: nas-server
Path: C:\GitFile\nas-server
Purpose: Short, practical briefing for the next Codex session.

## A. Recently Completed

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

## B. Test Status

### Unit tests
- Capacity service/handler unit tests: passing
- User service unit tests: passing
- Auth service unit tests: passing

### Integration tests (Spring + real DB)
- UserServiceIntegrationTest: passing
- AuthControllerIntegrationTest: passing
- CapacityAllocationBatchIntegrationTest: passing
- BatchJobWorkerCapacityIntegrationTest: passing

## C. Constraints / Environment Notes
- DB config comes from src/main/resources/db.properties.
- Tests are designed to run with real DB (`@AutoConfigureTestDatabase(replace = NONE)` where applicable).
- Audit columns are not-null; `AuditorAwareImpl` must return a valid system id (currently 1).

## D. Suggested Immediate Next Work
1. Add register/change-password controller endpoints for user domain.
2. Add login-required authorization policy (currently SecurityConfig permits all requests).
3. Add email-link based password reset flow (token table + mail sender + validation).
4. Add admin recovery on boot as per requirement docs.

## D-1. Open Issues / TODO Priority
1. Authorization boundaries are not applied yet (`permitAll` globally in SecurityConfig).
2. User register/password-change API endpoints are missing (service methods already exist).
3. Requirement-level password reset (email link) is still missing.
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
