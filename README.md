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

## Current Status (2026-02-11 기준)
- 세션 기반 로그인/로그아웃 API 구현
  - `POST /api/auth/login`
  - `POST /api/auth/logout`
- 사용자 도메인 서비스 구현
  - 회원 등록(`UserService.register`)
  - 비밀번호 변경(`UserService.changePassword`)
  - 비밀번호 BCrypt 해시 저장
- 용량 부여/회수는 직접 반영하지 않고 배치 큐를 통해 적용
- 배치 워커 시작 시점에만 `started_at` 기록

## Project Structure
```text
nas-server/
|-- build.gradle
|-- settings.gradle
|-- reference/
|   `-- codex/
|       |-- PROJECT_HANDOFF.md
|       |-- SESSION_BRIEF.md
|       |-- NEXT_SESSION_PROMPT.md
|       `-- CODEX_DOC_GUIDE.md
`-- src/
    |-- main/
    |   |-- java/io/github/supernoobchallenge/nasserver/
    |   |   |-- NasServerApplication.java
    |   |   |-- batch/
    |   |   |   |-- config/
    |   |   |   |-- entity/
    |   |   |   |-- handler/
    |   |   |   |   `-- impl/
    |   |   |   |-- repository/
    |   |   |   |-- scheduler/
    |   |   |   `-- service/
    |   |   |-- file/
    |   |   |   |-- capacity/
    |   |   |   |   |-- entity/
    |   |   |   |   |-- repository/
    |   |   |   |   `-- service/
    |   |   |   |-- core/
    |   |   |   |   |-- entity/
    |   |   |   |   `-- repository/
    |   |   |   `-- transfer/
    |   |   |       `-- entity/
    |   |   |-- global/
    |   |   |   |-- config/
    |   |   |   |-- entity/
    |   |   |   `-- security/
    |   |   |-- group/
    |   |   |   `-- entity/
    |   |   |-- share/
    |   |   |   `-- entity/
    |   |   `-- user/
    |   |       |-- controller/
    |   |       |-- dto/
    |   |       |-- entity/
    |   |       |-- repository/
    |   |       `-- service/
    |   `-- resources/
    |       |-- application.properties
    |       |-- db.properties
    |       `-- schema (2)1.sql
    `-- test/java/io/github/supernoobchallenge/nasserver/
        |-- batch/
        |-- connection/
        |-- file/
        |-- repository/
        `-- user/
```

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
.\gradlew.bat test --tests "*UserServiceIntegrationTest" --tests "*AuthControllerIntegrationTest"
.\gradlew.bat test --tests "*CapacityAllocationBatchIntegrationTest" --tests "*BatchJobWorkerCapacityIntegrationTest"
```

## Configuration Notes
- DB 설정은 `src/main/resources/db.properties` 사용
- 일부 통합 테스트는 실제 DB 연결(`@AutoConfigureTestDatabase(replace = NONE)`) 기준
- 감사(Audit) 시스템 사용자 ID는 `1`로 고정

## Next Priorities
1. 사용자 등록/비밀번호 변경 Controller API 추가
2. Security 정책 세분화(공개/인증 API 분리)
3. 이메일 링크 기반 비밀번호 재설정 플로우 구현
4. 부팅 시 관리자 복구 기능 구현