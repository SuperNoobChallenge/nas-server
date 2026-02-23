# NEXT_SESSION_PROMPT

## Role
너는 `C:\GitFile\nas-server` 유지보수/기능 확장을 담당하는 Codex다.

## Current Snapshot (Latest Only)
- 가입 공개 경로는 `POST /api/users/invite-register`만 허용된다.
- `POST /api/share-links/invites`는 로그인 사용자만 가능하며 inviter는 세션 사용자 ID 기반이다.
- Spring Security 공개 경계:
  - `POST /api/auth/login`
  - `POST /api/users/invite-register`
  - `POST /api/users/password-reset/request`
  - `POST /api/users/password-reset/confirm`
- 그 외 API는 인증 필요.
- 서비스 관련 클래스/인터페이스 파일명은 `*Service` 규칙을 따른다.
- 엔티티 `@Id` 필드 변수명은 모두 `id`로 통일되어 있다.
- 배치 관련 최근 이슈 정리:
  - 워커 폴링 쿼리 수정은 원인 해결(root-cause fix)
  - 용량 워커 통합테스트의 `nextRunAt` 강제 과거 설정은 테스트 안정화(hardening)
- Thymeleaf 웹 화면 추가:
  - `/web/login` 로그인
  - `/web/directories` 디렉터리 트리 관리(생성/이름변경/이동/삭제요청)

## First Steps
1. `reference/codex/SESSION_BRIEF.md`, `reference/codex/PROJECT_HANDOFF.md`, `reference/codex/SESSION_LOG_SHORT.md`, `reference/TODO.md` 순서로 읽는다.
2. 아래 핵심 파일을 확인한다:
   - `src/main/java/io/github/supernoobchallenge/nasserver/global/config/SecurityConfig.java`
   - `src/main/java/io/github/supernoobchallenge/nasserver/user/service/AuthService.java`
   - `src/main/java/io/github/supernoobchallenge/nasserver/user/controller/UserController.java`
   - `src/main/java/io/github/supernoobchallenge/nasserver/user/service/PasswordResetService.java`
   - `src/main/java/io/github/supernoobchallenge/nasserver/share/controller/ShareInvitationController.java`
   - `src/main/java/io/github/supernoobchallenge/nasserver/file/core/repository/VirtualFileRepository.java`
   - `src/main/java/io/github/supernoobchallenge/nasserver/file/core/repository/RealFileRepository.java`
3. 기준선 테스트를 실행한다:
   - `.\gradlew.bat test --tests "*AuthControllerIntegrationTest" --tests "*UserControllerIntegrationTest" --tests "*ShareInvitationIntegrationTest" --tests "*UserServiceIntegrationTest" --tests "*VirtualFileDeleteHandlerTest" --tests "*VirtualDirectoryDeleteHandlerTest"`
   - `.\gradlew.bat test --tests "*BatchJobWorkerCapacityIntegrationTest" --tests "*CapacityAllocationBatchIntegrationTest"`

## Priority
1. 관리자 복구 기능(부팅 시)
2. 용량 요청 배치 API 레이어
3. 배치 상태/타입 enum화 및 네이밍 정합성
4. 비밀번호 재설정 토큰 전용 테이블 + 실제 메일 발송 어댑터 연결

## Rule
- 작업 종료 시 `SESSION_BRIEF.md`, `PROJECT_HANDOFF.md`, `SESSION_LOG_SHORT.md`를 최신 사실로 즉시 갱신한다.
- 스냅샷/우선순위가 바뀌면 `NEXT_SESSION_PROMPT.md`도 함께 갱신한다.
- 도메인 패키지 README 작업 요청 시 `reference/codex/DOMAIN_README_STANDARD.md` 절차를 따른다.
- `reference/TODO.md`는 사용자 지시가 없어도 현재 작업 기준으로 직접 판단해 상시 갱신한다(신규 TODO 추가, 완료 항목 정리, 우선순위 재배열, Updated 날짜 갱신).
