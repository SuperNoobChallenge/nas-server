# nas-server

개인 NAS 백엔드 서버 프로젝트입니다.  
대용량 파일 저장/가상 파일 시스템/용량 할당/사용자 인증을 중심으로 개발 중입니다.

## Tech Stack
- Java 25 (Gradle Toolchain)
- Spring Boot 4.0.2
- Spring Data JPA
- Spring Security
- MySQL / MariaDB Driver
- JUnit 5

## Current Status (2026-02-12 기준)
- 세션 기반 로그인/로그아웃 API 구현
  - `POST /api/auth/login`
  - `POST /api/auth/logout`
- 사용자 가입 정책
  - 공개 직접 가입(`POST /api/users`) 비활성화
  - 공유 초대 링크 기반 가입만 허용: `POST /api/users/invite-register`
- 공유 초대 링크 API 구현
  - `POST /api/share-links/invites`
  - inviter는 요청값이 아니라 로그인 세션 사용자 ID 기반으로 결정
- 사용자 API 구현
  - `PATCH /api/users/{userId}/password`
- Spring Security 인가 경계 적용
  - 공개: `POST /api/auth/login`, `POST /api/users/invite-register`
  - 그 외 API: 인증 필요
- 로그인 시 Spring Security `SecurityContext`를 세션에 저장
- 용량 부여/회수는 직접 반영하지 않고 배치 큐를 통해 적용
- 배치 워커 시작 시점에만 `started_at` 기록

## Project Structure
```text
nas-server/
|-- build.gradle
|-- README.md
|-- gradlew
|-- gradlew.bat
|-- settings.gradle
|-- gradle/
|   `-- wrapper/
|       |-- gradle-wrapper.jar
|       `-- gradle-wrapper.properties
|-- reference/
|   `-- codex/
`-- src/
    |-- main/
    |   |-- java/io/github/supernoobchallenge/nasserver/
    |   |   |-- NasServerApplication.java
    |   |   |-- batch/
    |   |   |   |-- config/
    |   |   |   |-- entity/
    |   |   |   |-- handler/impl/
    |   |   |   |-- repository/
    |   |   |   |-- scheduler/
    |   |   |   `-- service/
    |   |   |-- file/
    |   |   |   |-- capacity/entity|repository|service/
    |   |   |   |-- core/entity|repository/
    |   |   |   `-- transfer/entity/
    |   |   |-- global/
    |   |   |   |-- config/
    |   |   |   |-- entity/
    |   |   |   `-- security/
    |   |   |-- group/entity/
    |   |   |-- share/
    |   |   |   |-- controller/
    |   |   |   |-- dto/
    |   |   |   |-- entity/
    |   |   |   |-- repository/
    |   |   |   `-- service/
    |   |   `-- user/
    |   |       |-- controller/
    |   |       |-- dto/
    |   |       |-- entity/
    |   |       |-- repository/
    |   |       `-- service/
    |   `-- resources/
    |       |-- application.properties
    |       `-- schema (2)1.sql
    `-- test/java/io/github/supernoobchallenge/nasserver/
        |-- NasServerApplicationTests.java
        |-- batch/handler/impl/
        |-- batch/scheduler/
        |-- connection/
        |-- file/capacity/integration/
        |-- file/capacity/service/
        |-- repository/
        |-- share/integration/
        |-- user/integration/
        `-- user/service/
```

Note: 위 구조는 `.gitignore` 기준으로 GitHub에 올라가지 않는 항목(예: `build/`, `.gradle/`, `.idea/`, `db.properties`, `reference/0*`)을 제외해 작성했습니다.

## Run
Windows 기준:
```powershell
.\gradlew.bat bootRun
```

## Test
전체 테스트:
```powershell
.\gradlew.bat test
```

최근 핵심 테스트 예시:
```powershell
.\gradlew.bat test --tests "*AuthControllerIntegrationTest" --tests "*UserControllerIntegrationTest" --tests "*ShareInvitationIntegrationTest" --tests "*UserServiceIntegrationTest"
.\gradlew.bat test --tests "*CapacityAllocationBatchIntegrationTest" --tests "*BatchJobWorkerCapacityIntegrationTest"
```

## Configuration Notes
- `application.properties`에서 `spring.config.import=optional:classpath:db.properties`를 사용
- `db.properties`는 `.gitignore`에 포함되어 GitHub에 올라가지 않음 (로컬 환경에서 별도 관리)
- 일부 통합 테스트는 실제 DB 연결(`@AutoConfigureTestDatabase(replace = NONE)`) 기준
- 감사(Audit) 기본 시스템 사용자 ID는 `1`

## Next Priorities
1. 이메일 링크 기반 비밀번호 재설정 플로우 구현
2. 부팅 시 관리자 복구 기능 구현
3. 용량 요청 배치 API 레이어 추가
4. 배치 모델 enum화 및 네이밍 정합성 리팩터링
