# README_STANDARD

Purpose: 루트 `README.md` 생성/수정 시 일관된 기준을 강제한다.

## 1. 범위
- 이 문서는 저장소 루트 `README.md` 작성 규칙이다.
- 본문은 현재 코드 기준 "사실"만 작성한다. (추측/예정 작업 금지)

## 2. Project Structure 작성 규칙 (핵심)
- 반드시 `.gitignore`를 먼저 확인하고, GitHub에 올라가지 않는 항목은 구조에서 제외한다.
- 구조는 **폴더명만** 작성한다. 파일명은 쓰지 않는다.
- 프로젝트 이해에 직접 필요 없는 빌드/도구 항목은 제외한다.
  - 제외 예시: `gradle/`, `.gradle/`, `build/`, `.idea/`, `.vscode/`, `.codex/`
- 비공개/로컬 설정으로 관리되는 항목은 제외한다.
  - 예시: `db.properties`, `db.yml`
- 구조는 "실제 운영 코드 파악" 기준으로 최소한만 남긴다.
  - 포함 예시: `src/main/java`, `src/main/resources`, `src/test/java`, `reference/codex`

## 3. Structure 표기 형식
- 코드블록은 `text` 트리 형식을 사용한다.
- 루트부터 하위로 내려가되, 깊이는 필요한 만큼만 유지한다.
- 도메인 레벨은 폴더 단위로만 표시한다.
  - 예: `user/controller`, `share/service`, `batch/repository`
- 중간 패키지는 압축 표기를 허용한다.
  - 예: `java/io/github/supernoobchallenge/nasserver/`
- 압축 표기를 사용해도, 각 도메인 내부 하위 폴더는 실제 구조가 보이도록 펼쳐서 적는다.

## 4. 체크리스트
- [ ] `.gitignore` 확인 완료
- [ ] Project Structure에 파일명 미포함
- [ ] Project Structure에 불필요한 빌드/도구 폴더 미포함
- [ ] GitHub 비업로드 항목 미포함
- [ ] 현재 코드 상태와 README 내용 일치

## 5. 권장 Project Structure 예시 (중간 패키지 압축 + 하위 폴더 표시)
```text
nas-server/
|-- reference/
|   `-- codex/
`-- src/
    |-- main/
    |   |-- java/io/github/supernoobchallenge/nasserver/
    |   |   |-- batch/
    |   |   |-- file/
    |   |   |-- global/
    |   |   |-- group/
    |   |   |-- share/
    |   |   `-- user/
    |   `-- resources/
    `-- test/
        `-- java/io/github/supernoobchallenge/nasserver/
            |-- batch/
            |-- file/
            |-- repository/
            |-- share/
            `-- user/
```
