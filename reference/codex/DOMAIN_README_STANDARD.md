# DOMAIN_README_STANDARD

Purpose: 도메인 패키지(`batch`, `file`, `user` 등) 내부 `README.md`를 다음 세션에서도 동일한 품질로 작성/갱신하기 위한 실행 지침.
Audience: LLM/Codex only.

## 1. Scope
- 대상 파일:
  - `src/main/java/io/github/supernoobchallenge/nasserver/<domain>/README.md`
- 대상 도메인:
  - `batch`, `file`, `global`, `group`, `share`, `user` (필요 시 신규 도메인 포함)
- 목표:
  - 단순 폴더 설명이 아니라, 메서드 호출 흐름과 도메인 간 상호작용을 명확히 문서화한다.

## 2. Trigger (언제 이 지침을 사용하나)
- 사용자가 아래 요청을 하면 반드시 사용:
  - "도메인 README 만들어줘/업데이트해줘"
  - "기능별 디렉터리 설명 문서화해줘"
  - "메서드 상호작용까지 README에 적어줘"

## 3. Pre-Research Rules (필수 사전 조사)
- 추측 금지. 코드에서 확인한 사실만 기록.
- 아래 순서로 조사:
1. 도메인 디렉터리 목록 확인
2. 각 도메인의 `controller/service/scheduler/handler/repository/entity` 존재 여부 확인
3. public 메서드와 핵심 내부 호출 관계 추출
4. 도메인 간 연결 지점 확인
   - 예: `user -> share`, `file -> batch`, `global -> user`

권장 명령:
```powershell
Get-ChildItem src/main/java/io/github/supernoobchallenge/nasserver -Directory
rg --line-number --glob "*.java" "public [^\n]*\(" src/main/java/io/github/supernoobchallenge/nasserver/<domain>
rg --files src/main/java/io/github/supernoobchallenge/nasserver/<domain>
```

## 4. README 작성 템플릿 (필수 구조)
- 각 도메인 README는 아래 섹션 순서를 기본으로 사용한다.

```md
# <domain> Domain Package

<도메인 한 줄 설명>

## 구성 요소 역할
- <패키지/클래스>: <역할>

## 주요 메서드 상호작용
### 1) <대표 시나리오>
1. <진입 메서드>
2. <서비스 메서드>
3. <리포지토리/엔티티 메서드>
4. <상태 변경/결과>

핵심: <이 시나리오의 핵심 제약 또는 정책>

### 2) <두 번째 시나리오>
...

## 실패/검증 포인트
- <검증/예외 처리 지점>

## 현재 상태 메모
- <미구현/엔티티 중심 상태/추가 필요 사항>
```

## 5. Writing Rules (품질 기준)
- 메서드명은 실제 코드 식별자 그대로 사용 (`UserService.registerInvitedUser`).
- 호출 순서는 "진입점 -> 서비스 -> 리포지토리/엔티티 -> 결과"로 명시.
- 도메인 간 위임 지점을 분리해 적는다.
  - 예: "`file` 도메인은 실제 반영을 `batch` 핸들러에 위임"
- 성공 경로만 쓰지 말고 실패/검증 포인트도 최소 1개 이상 명시.
- 현재 미구현 영역(`group`의 API/서비스 부재 등)은 상태 메모에 사실 그대로 기록.

## 6. Update Procedure (실행 순서)
1. 기존 README 존재 여부 확인 (`Test-Path .../README.md`)
2. 없으면 생성, 있으면 템플릿 구조로 확장/정렬
3. 도메인별 메서드 흐름 검증 후 문서 반영
4. 최종 확인:
   - 모든 도메인 README 제목/도메인명이 일치하는지
   - 호출 흐름에 실제 존재하는 메서드만 적었는지
   - 추측 문장이 없는지

검증 명령:
```powershell
Get-ChildItem src/main/java/io/github/supernoobchallenge/nasserver -Filter README.md -Recurse
```

## 7. Completion Checklist
- [ ] 도메인 README 파일 생성/갱신 완료
- [ ] 각 README에 "구성 요소 역할" 포함
- [ ] 각 README에 "주요 메서드 상호작용" 포함
- [ ] 도메인 간 위임/연계 지점 명시
- [ ] 실패/검증 포인트 명시
- [ ] 사실 기반(코드 검증) 문장만 사용
