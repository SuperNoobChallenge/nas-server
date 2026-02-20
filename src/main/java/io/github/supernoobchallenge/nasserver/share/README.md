# share Domain Package

초대 링크 및 공유 링크 모델을 관리하며, 링크 검증을 통해 사용자 가입/재설정 흐름과 연결되는 도메인입니다.

## 구성 요소 역할
- `controller/ShareInvitationController`
  - 초대 링크 생성 API 진입점입니다.
- `service/ShareInvitationService`
  - 초대 링크 생성 및 링크 기반 가입 검증/연계를 처리합니다.
- `repository/ShareLinkRepository`
  - `shareUuid`, 사용자별 링크 타입 기준 조회를 제공합니다.
- `entity/ShareLink`
  - 링크 타입, 만료, 사용 횟수, 비밀번호 보호 여부를 관리합니다.
- `entity/ShareDirectory`, `entity/ShareFile`
  - 공유 링크 하위 트리 구조를 위한 엔티티 모델입니다.
- `dto/*`
  - 초대 링크 생성 요청/응답 모델입니다.

## 주요 메서드 상호작용

### 1) 초대 링크 생성 흐름
1. `ShareInvitationController.createInviteLink(...)` 호출
2. `AuditorAwareImpl.getAuthenticatedAuditor()`로 로그인 사용자 ID 획득
3. `ShareInvitationService.createInviteLink(...)` 호출
4. `UserRepository.findById(inviterUserId)`로 inviter 검증
5. 비밀번호가 있으면 `PasswordEncoder.encode(...)`
6. `ShareLink.createInviteLink(...)`로 도메인 객체 생성
7. `ShareLinkRepository.save(...)` 저장 후 `CreateInviteLinkResponse` 반환

핵심: inviter는 요청 바디가 아니라 세션 사용자에서 결정됩니다.

### 2) 초대 링크 기반 가입 흐름
1. `UserController.inviteRegister(...)`에서 `ShareInvitationService.registerByInviteLink(...)` 호출
2. `ShareLinkRepository.findByShareUuid(shareUuid)`로 링크 조회
3. `validateInviteLinkUsable(...)`에서 아래 검증 수행
   - `shareLink.isDeleted()`
   - `shareLink.isInviteLink()`
   - `shareLink.isExpired(now)`
   - `shareLink.isExhausted()`
   - 비밀번호 링크인 경우 `PasswordEncoder.matches(...)`
4. 검증 통과 시 `UserService.registerInvitedUser(..., shareLink.getUser().getId())` 호출
5. 가입 성공 후 `shareLink.increaseUseCount()`로 사용 횟수 증가

핵심: 링크 검증이 선행되고, 사용자 생성은 `user` 도메인 서비스로 위임됩니다.

### 3) 비밀번호 재설정과의 공유 포인트
1. `PasswordResetService`가 `ShareLink.createPasswordResetLink(...)`로 토큰 링크 생성
2. `ShareLinkRepository.findAllByUser_IdAndLinkTypeAndDeletedAtIsNull(...)`로 기존 활성 토큰 무효화 대상 조회
3. 확정 시 `findByShareUuid(token)` 조회 후
   - `isPasswordResetLink`, `isExpired`, `isExhausted`, `isDeleted` 검증
   - `increaseUseCount()` + `delete()`로 1회성 소비

핵심: 초대 링크와 비밀번호 재설정 링크가 동일한 `ShareLink` 모델을 재사용합니다.
