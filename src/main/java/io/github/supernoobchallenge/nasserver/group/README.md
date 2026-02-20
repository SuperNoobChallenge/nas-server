# group Domain Package

그룹과 그룹 구성원 권한 모델을 정의하는 도메인입니다. 현재는 엔티티 중심으로 구성되어 있습니다.

## 구성 요소 역할
- `entity/Group`
  - 그룹 기본 정보와 그룹의 `FilePermissionKey`, 소유자(`User`)를 연결합니다.
- `entity/GroupUser`
  - 그룹-사용자 소속 관계와 읽기/쓰기 레벨을 저장합니다.
- `entity/GroupUserPermission`
  - `GroupUser`와 1:1로 연결되어 그룹 내 관리 권한(`canManageUser`, `canManageFile`)을 저장합니다.
- `entity/GroupInvite`
  - `ShareLink`와 `Group`을 연결하는 초대 매핑입니다.

## 현재 기준 상호작용 모델

### 1) 그룹 멤버십/권한 연결 구조
1. `Group`이 생성되면 그룹의 저장소 권한 루트(`filePermission`)와 소유자(`owner`)를 가집니다.
2. 멤버 추가 시 `GroupUser`가 생성되어 `Group`과 `User`를 연결합니다.
3. 멤버별 상세 권한은 `GroupUserPermission`이 `@MapsId`로 `GroupUser` PK를 공유해 1:1로 붙습니다.

핵심: 그룹 소속과 그룹 내 세부 권한이 분리 저장됩니다.

### 2) 그룹 초대 링크 연계 구조
1. 초대 링크는 `share` 도메인의 `ShareLink`에서 발급됩니다.
2. `GroupInvite`가 `share_link_id`를 PK로 공유(`@MapsId`)하며 `Group`과 연결됩니다.

핵심: 초대 토큰 수명/사용 횟수 정책은 `share` 도메인에 위임하고, `group`은 매핑만 담당합니다.

## 상태 메모
- 현재 `group` 도메인에는 컨트롤러/서비스/리포지토리가 없습니다.
- 실제 생성/초대/가입 플로우는 아직 구현되지 않았으며, 엔티티 관계 정의가 우선 반영된 상태입니다.
