# TODO

Updated: 2026-02-23
Scope: Domain-based backlog

## Domain Index
- `user`
- (reserved) `share`
- (reserved) `file`
- (reserved) `batch`
- (reserved) `group`
- (reserved) `global`

## user
### P1 (High)
- [ ] `PATCH /api/users/{userId}/password`에서 세션 사용자와 path `userId` 일치 검증 추가
- [ ] 로그인 실패/입력 오류 시 `AuthController` 일관 에러 응답 처리 (`400` 또는 `401` 정책 확정 후 반영)
- [ ] 초대가입/비밀번호재설정 토큰 사용 경합 방지 (락/원자 업데이트 기반)
- [ ] 비밀번호 재설정 메일 실제 발송 어댑터(SMTP 또는 외부 provider) 구현 및 설정 연결

### P2 (Medium)
- [ ] 초대 링크에 초대 대상 이메일 바인딩 정책 도입 (스키마 + API + 검증)
- [ ] 사용자 관련 DTO에 Bean Validation 적용 (`@Valid`, `@Email`, 길이/패턴)
- [ ] 사용하지 않는 DTO(`RegisterUserRequest`, `RegisterUserResponse`) 정리 또는 재활용 방향 확정

## share
### P1 (High)
- [ ] 없음

### P2 (Medium)
- [ ] 없음

## file
### P1 (High)
- [ ] 디렉터리 이동 시 하위 트리 전체 `depthLevel` 연쇄 갱신 로직 추가
- [ ] `virtual_directory_stats`를 디렉터리/파일 변경 이벤트(생성·이동·삭제)와 동기화
- [ ] 디렉터리 생성 동시성 중복(같은 parent/name 경쟁 요청) 방지 전략 적용 (락 또는 DB 유니크 제약)

### P2 (Medium)
- [ ] 디렉터리 삭제 요청 멱등성/중복 enqueue 제어 (`DIRECTORY_DELETE` 중복 등록 방지)
- [ ] `readLevel/writeLevel` 기반 사용자 접근 제어 로직 확장 (현재 소유 `filePermission` 경계만 검증)
- [ ] 디렉터리 권한 변경 API(`updateAccessLevel`) 추가

## batch
### P1 (High)
- [ ] 없음

### P2 (Medium)
- [ ] 없음

## group
### P1 (High)
- [ ] 없음

### P2 (Medium)
- [ ] 없음

## global
### P1 (High)
- [ ] 없음

### P2 (Medium)
- [ ] Thymeleaf 웹 폼 경로(`/web/**`)에 대한 CSRF 정책 확정 및 적용(재활성화 시 템플릿 토큰 반영 포함)

## Note
- 신규 도메인 추가 시 `Domain Index`에 이름을 추가하고 동일한 `P1/P2` 섹션으로 확장
- 기준: 현재 코드 점검 결과
