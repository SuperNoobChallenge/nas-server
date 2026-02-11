# NEXT_SESSION_PROMPT

아래 지시를 그대로 따르며 작업을 이어가라.

## Role
너는 이 프로젝트의 유지보수/기능 확장을 담당하는 Codex다.
프로젝트 루트는 `C:\GitFile\nas-server`다.

## First Steps
1. `reference/codex/PROJECT_HANDOFF.md`와 `reference/codex/SESSION_BRIEF.md`를 먼저 읽어 현재 상태를 파악한다.
2. 최근 변경 코드의 핵심 파일을 확인한다:
   - `src/main/java/io/github/supernoobchallenge/nasserver/user/service/UserService.java`
   - `src/main/java/io/github/supernoobchallenge/nasserver/user/service/AuthService.java`
   - `src/main/java/io/github/supernoobchallenge/nasserver/user/controller/AuthController.java`
   - `src/main/java/io/github/supernoobchallenge/nasserver/batch/repository/BatchJobQueueRepository.java`
3. 테스트를 먼저 한 번 돌려 기준선 상태를 확인한다.

## Current Truths (Do Not Break)
- 시스템 감사 사용자 ID는 `1`이다.
- user 생성 시 `UserPermission`은 반드시 함께 생성/연결되어야 한다.
- 비밀번호는 해시(BCrypt)로 저장되어야 한다.
- 용량 부여/회수는 배치 큐를 통해서만 반영된다.
- 배치 started_at은 작업 시작 시점에만 기록된다(초기 wait 상태는 null 가능).

## Quality Bar
- 기능 추가 시 단위 테스트 + 가능한 범위의 통합 테스트를 함께 작성한다.
- DB 스키마/엔티티 불일치가 생기면 원인과 수정 방향을 명확히 남긴다.
- 기존 동작 회귀를 만들지 않는다.

## Priority Backlog
1. User 등록/비밀번호 변경 API(controller) 추가 및 검증.
2. 보안 정책 정교화: 인증 필요한 API와 공개 API 분리.
3. 이메일 링크 기반 비밀번호 재설정 플로우 구현.
4. 관리자 복구 기능(부팅 시 플래그 기반) 설계/구현.

## Execution Notes
- 테스트는 실제 스프링 + DB 검증을 선호한다.
- 필요 시 `@AutoConfigureTestDatabase(replace = NONE)`를 사용한다.
- 작업이 끝나면 `reference/codex/SESSION_BRIEF.md`와 `reference/codex/PROJECT_HANDOFF.md`를 갱신한다.
