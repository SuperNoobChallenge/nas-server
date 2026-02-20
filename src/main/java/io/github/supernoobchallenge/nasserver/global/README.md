# global Domain Package

보안 경계, JPA 감사(auditing), 공통 엔티티 규칙을 제공하는 공통 도메인입니다.

## 구성 요소 역할
- `config/SecurityConfig`
  - 공개 API와 인증 필요 API를 분리하고, 세션 기반 인증 정책을 설정합니다.
  - `PasswordEncoder`(BCrypt)를 빈으로 제공합니다.
- `config/JpaConfig`
  - `@EnableJpaAuditing`으로 생성/수정 감사값 자동 주입을 활성화합니다.
- `security/AuditorAwareImpl`
  - 현재 요청의 세션에서 로그인 사용자 ID를 읽어 감사 주체(`createdBy`, `updatedBy`)를 제공합니다.
  - 세션 사용자가 없으면 시스템 사용자 ID(`1`)를 fallback으로 사용합니다.
- `entity/AuditEntity`, `entity/BaseEntity`
  - 모든 도메인이 공유하는 감사 컬럼/soft delete 공통 메서드를 제공합니다.

## 주요 메서드 상호작용

### 1) 인증 경계 + 세션 컨텍스트 흐름
1. `SecurityConfig.filterChain(...)`이 API 접근 규칙을 설정
2. `AuthService.login(...)`이 인증 성공 시
   - 세션에 `LOGIN_USER_ID`, `LOGIN_ID` 저장
   - `SecurityContext`를 생성해 세션(`SPRING_SECURITY_CONTEXT`)에 저장
3. 이후 요청에서 `AuditorAwareImpl.getAuthenticatedAuditor()`가 세션의 `LOGIN_USER_ID`를 읽음

핵심: 인증 도메인(`user`)과 감사 도메인(`global`)이 세션 사용자 ID로 연결됩니다.

### 2) JPA 감사값 자동 채움 흐름
1. 엔티티가 `AuditEntity`를 상속하면 `createdAt/updatedAt/createdBy/updatedBy` 대상이 됨
2. 저장/수정 시점에 JPA auditing이 `AuditorAwareImpl.getCurrentAuditor()` 호출
3. 로그인 사용자 ID가 있으면 해당 값, 없으면 `SYSTEM_USER_ID=1`이 감사값으로 기록

### 3) soft delete 공통 동작 흐름
1. 도메인 엔티티가 `BaseEntity`를 상속
2. 도메인 서비스/핸들러에서 `delete()` 호출 시 `deletedAt`이 설정
3. 비즈니스 로직에서 `isDeleted()`로 활성/삭제 상태를 판별

핵심: `share`, `user`, `file` 등 여러 도메인이 같은 삭제/감사 규칙을 공유합니다.
