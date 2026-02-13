# NAMING_STANDARD

Purpose: 클래스/메서드/API/테스트 명명 혼선을 줄인다.

## 1. 클래스명
- Controller: `XxxController`
- Service: `XxxService`
- Service 인터페이스/구현 파일명도 모두 `*Service`로 끝낸다.
- Repository: `XxxRepository`
- Entity: 도메인 명사 단수형 (`User`, `ShareLink`)
- DTO: `XxxRequest`, `XxxResponse`

## 2. 메서드명
- 동사 + 목적어 형태 사용 (`createInviteLink`, `registerInvitedUser`)
- boolean 반환은 `is/has/can` 접두 사용 (`isExpired`, `hasPassword`)
- 예외 상황이 명확한 이름 사용 (`validateInviteLinkUsable`)

## 3. API 경로
- 복수형 리소스 사용: `/api/users`, `/api/share-links`
- 행위가 필요하면 하위 경로로 표현: `/invite-register`, `/password`
- path variable 이름은 리소스 id와 동일: `{userId}`, `{shareLinkId}`

## 4. 테스트명
- 패턴: `method_condition_expected`
- `@DisplayName`은 한국어 문장으로 의도를 명확히 작성

## 5. 엔티티 식별자 필드명
- `@Id`가 붙은 엔티티 필드 변수명은 항상 `id`를 사용한다.
- DB 컬럼명이 `share_link_id`, `virtual_file_id`처럼 달라도 자바 필드명은 `id`를 유지한다.
