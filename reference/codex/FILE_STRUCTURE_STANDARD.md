# FILE_STRUCTURE_STANDARD

Purpose: 프로젝트 파일 배치 기준을 고정해, 세션마다 구조가 흔들리지 않게 한다.

## 1. Backend 기본 구조
```text
src/main/java/io/github/supernoobchallenge/nasserver
  ├─ <domain>/
  │   ├─ controller/
  │   ├─ dto/
  │   ├─ entity/
  │   ├─ repository/
  │   └─ service/
  ├─ global/
  ├─ batch/
  └─ ...
```

## 2. 테스트 구조
```text
src/test/java/io/github/supernoobchallenge/nasserver
  ├─ <domain>/service/         (unit test)
  ├─ <domain>/integration/     (spring + db integration)
  └─ ...
```

## 3. 문서 구조
```text
reference/codex
  ├─ PROJECT_HANDOFF.md
  ├─ SESSION_BRIEF.md
  ├─ NEXT_SESSION_PROMPT.md
  ├─ CODEX_DOC_GUIDE.md
  └─ (운영 표준 문서들)
```

## 4. 금지 패턴
- 도메인 로직을 `controller`에 작성하지 않는다.
- 공통 설정/보안/감사는 `global` 밖으로 분산하지 않는다.
- 테스트 유틸을 프로덕션 코드에 두지 않는다.
