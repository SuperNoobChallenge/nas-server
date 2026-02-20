# user Domain Package

인증, 초대 기반 회원가입, 비밀번호 변경, 비밀번호 재설정 흐름을 담당하는 도메인입니다.

## 구성 요소 역할
- `controller/AuthController`
  - 로그인/로그아웃 API 진입점입니다.
- `controller/UserController`
  - 초대 가입, 비밀번호 변경, 비밀번호 재설정 요청/확정 API 진입점입니다.
  - `IllegalArgumentException`을 일관된 `400` 에러 응답으로 매핑합니다.
- `service/AuthService`
  - 로그인 검증, 세션 재생성, SecurityContext 저장/제거를 담당합니다.
- `service/UserService`
  - 사용자 생성(일반/초대), 권한키/사용자권한 초기화, 비밀번호 변경을 담당합니다.
- `service/PasswordResetService`
  - 이메일 기반 토큰 발급, 토큰 검증, 비밀번호 갱신, 토큰 소비를 담당합니다.
- `repository/UserRepository`, `UserPermissionRepository`
  - 사용자/권한 영속화를 담당합니다.
- `entity/User`, `UserPermission`
  - 사용자와 사용자 권한 모델입니다.
- `service/PasswordResetMailService` + `LoggingPasswordResetMailService`
  - 재설정 메일 발송 인터페이스와 기본 로깅 구현입니다.

## 주요 메서드 상호작용

### 1) 로그인/로그아웃 흐름
1. `AuthController.login(...)` -> `AuthService.login(...)`
2. `UserRepository.findByLoginId(loginId)`로 사용자 조회
3. `user.isDeleted()` 및 `PasswordEncoder.matches(rawPassword, user.getPassword())` 검증
4. 기존 세션 무효화 후 새 세션 생성
5. 세션에 `LOGIN_USER_ID`, `LOGIN_ID` 저장
6. `SecurityContext`를 생성해 세션(`SPRING_SECURITY_CONTEXT`)에 저장
7. `AuthController.logout(...)` -> `AuthService.logout(...)`에서 `SecurityContext` 정리 + 세션 invalidate

핵심: 인증 정보는 세션과 Spring Security 컨텍스트에 함께 기록됩니다.

### 2) 초대 가입 흐름
1. `UserController.inviteRegister(...)` 호출
2. `ShareInvitationService.registerByInviteLink(...)`에서 링크 검증 수행
3. 검증 통과 후 `UserService.registerInvitedUser(...)` 호출
4. `UserService.registerInternal(...)` 내부에서
   - 입력값 검증
   - `existsByLoginId`, `existsByEmail` 중복 검사
   - `FilePermissionKey` 생성 및 저장
   - `User` 생성(초대한 사용자 연결 포함)
   - `UserPermission` 생성 후 `user.assignPermission(...)`
   - `userRepository.save(user)` 저장

핵심: 가입 시 `UserPermission`과 `FilePermissionKey`가 한 흐름으로 함께 생성됩니다.

### 3) 비밀번호 변경 흐름
1. `UserController.changePassword(...)` -> `UserService.changePassword(...)`
2. `UserRepository.findById(userId)` 조회
3. 현재 비밀번호 일치 여부 `passwordEncoder.matches(...)` 검증
4. 새 비밀번호가 기존과 동일한지 검증
5. `user.changePassword(passwordEncoder.encode(newRawPassword))` 적용

### 4) 비밀번호 재설정 흐름
- 요청 단계 (`requestPasswordReset`)
  1. `UserController.requestPasswordReset(...)` -> `PasswordResetService.requestPasswordReset(email)`
  2. `UserRepository.findByEmail(email)` 조회
  3. 사용자 미존재/삭제 상태면 조용히 종료
  4. `ShareLinkRepository.findAllByUser_IdAndLinkTypeAndDeletedAtIsNull(...)`로 기존 토큰 무효화
  5. 새 토큰 생성 후 `ShareLink.createPasswordResetLink(...)` 저장
  6. `PasswordResetMailService.sendPasswordResetEmail(...)` 호출
- 확정 단계 (`confirmPasswordReset`)
  1. `UserController.confirmPasswordReset(...)` -> `PasswordResetService.confirmPasswordReset(token, newPassword)`
  2. `ShareLinkRepository.findByShareUuid(token)`로 토큰 조회
  3. 타입/만료/사용횟수/삭제 상태 검증
  4. 사용자 비밀번호 변경 후 토큰 `increaseUseCount()` + `delete()` 처리

핵심: 토큰 저장소는 현재 `share_links`를 재사용합니다.
