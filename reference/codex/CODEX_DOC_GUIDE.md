# CODEX_DOC_GUIDE

Purpose: `reference/codex` 내부 문서를 일관되게 관리하기 위한 운영 지침.
Audience: LLM/Codex only.

## 1. Core Rule
- 기록할 내용이 생기면 **언제든지 즉시 수정 가능**하다.
- 세션 중간이라도 중요한 결정/버그/테스트 결과는 바로 반영한다.
- 문서는 "완료 보고서"가 아니라 "진행 중 운영 로그"로 취급한다.
- 이 폴더 문서에는 **중요한 API 키/토큰/비밀번호/시크릿 정보를 절대 기록하지 않는다**.
- 이 문서는 Git 저장소에 **public으로 업로드된다**고 가정하고 작성한다.

## 2. File Roles
- `PROJECT_HANDOFF.md`
  - 장기 인수인계 문서 (전체 맥락, 주요 결정, 구조 변화, 누적 이력)
- `SESSION_BRIEF.md`
  - 다음 세션 진입용 요약 문서 (최근 변경/현재 상태/즉시 해야 할 일)
- `NEXT_SESSION_PROMPT.md`
  - 다음 Codex가 바로 실행할 수 있는 작업 프롬프트
- `CODEX_DOC_GUIDE.md` (this file)
  - 문서 자체의 관리 규칙
- `FILE_STRUCTURE_STANDARD.md`
  - 파일/패키지 배치 표준
- `COMPONENT_EXAMPLES.md`
  - controller/service/repository/test 최소 예시
- `CODING_RULES.md`
  - 구현/테스트/문서 작성 규칙
- `NAMING_STANDARD.md`
  - 클래스/메서드/API/테스트 명명 규칙
- `commonMistakes.md`
  - AI/Codex 반복 실수 방지 체크리스트
- `SESSION_LOG_SHORT.md`
  - 초단기 세션 로그 (한 줄 포맷)

## 3. Update Timing
- 아래 이벤트가 발생하면 문서를 업데이트한다.
1. 설계/정책이 바뀜 (예: system user id, 권한 정책)
2. 주요 기능 구현/삭제/리팩터링 완료
3. 중요한 버그 발견 또는 수정
4. 테스트 전략/실행 결과가 바뀜
5. 다음 세션 우선순위가 바뀜

## 4. Writing Rules
- 사실 기반으로 작성한다. (추측 금지)
- 경로/클래스/명령은 가능한 한 구체적으로 적는다.
- "무엇을 왜 바꿨는지"를 1~2줄로 남긴다.
- TODO는 우선순위를 숫자로 관리한다.
- 이미 오래된 정보는 삭제하거나 Deprecated 표시를 남긴다.

## 5. Minimal Update Checklist (Session End)
1. `SESSION_BRIEF.md` 업데이트
2. `PROJECT_HANDOFF.md` 동기화
3. `Open Issues / TODO Priority` 재정렬
4. 필요 시 `NEXT_SESSION_PROMPT.md` 갱신

## 6. Conflict Rule
- `SESSION_BRIEF.md`와 `PROJECT_HANDOFF.md`가 충돌하면:
  - 최신 사실 기준으로 두 파일을 즉시 동기화한다.
  - 충돌 상태를 방치하지 않는다.

## 7. Quality Bar
- 문서만 읽고도 다음 세션에서 바로 작업을 시작할 수 있어야 한다.
- 최소 포함 항목:
  - 현재 동작하는 것
  - 아직 안 된 것
  - 다음 우선순위
  - 재검증 명령
