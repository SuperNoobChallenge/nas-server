CREATE TABLE `users` (
  `user_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT COMMENT 'PK, 자동증가',
  `file_permission_id` bigint unsigned NOT NULL COMMENT 'FK: 유저에게 할당된 루트 파일 권한',
  `inviter_id` bigint unsigned COMMENT 'FK: 나를 초대한 유저 (자기참조)',
  `login_id` varchar(255) UNIQUE NOT NULL COMMENT '로그인 아이디',
  `password` varchar(255) NOT NULL COMMENT '암호화된 비밀번호',
  `email` varchar(50) UNIQUE NOT NULL COMMENT '이메일',
  `deleted_at` datetime COMMENT '비활성화 시기 (Soft Delete)',
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `user_permissions` (
  `user_id` bigint unsigned PRIMARY KEY COMMENT 'PK & FK: users 테이블과 1:1',
  `can_invite_person` boolean NOT NULL DEFAULT false,
  `can_share_file` boolean NOT NULL DEFAULT false,
  `can_store_personal` boolean NOT NULL DEFAULT false,
  `can_create_group` boolean NOT NULL DEFAULT false,
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `file_permission_keys` (
  `file_permission_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `parent_permission_id` bigint unsigned COMMENT 'FK: 상위 권한 키 (용량 상속)',
  `owner_type` varchar(10) NOT NULL COMMENT 'USER, GROUP',
  `total_capacity` bigint NOT NULL DEFAULT 0 COMMENT '총 할당 용량(Byte)',
  `available_capacity` bigint NOT NULL DEFAULT 0 COMMENT '현재 사용 가능 용량(Byte)',
  `deleted_at` datetime,
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `capacity_allocations` (
  `allocation_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `granter_allocation_id` bigint unsigned COMMENT 'FK: 용량을 준 상위 할당 ID',
  `receiver_permission_id` bigint unsigned NOT NULL COMMENT 'FK: 용량을 받는 키',
  `giver_permission_id` bigint unsigned COMMENT 'FK: 용량을 주는 키 (System=NULL)',
  `allocated_size` bigint NOT NULL COMMENT '할당되는 용량',
  `expiration_date` datetime COMMENT '만료일',
  `allocation_type` varchar(20) NOT NULL COMMENT 'EVENT, GRANT, SYSTEM',
  `description` varchar(100) COMMENT '세부 내용',
  `deleted_at` datetime,
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `virtual_directories` (
  `directory_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `file_permission_id` bigint unsigned NOT NULL COMMENT 'FK: 소속 권한 키',
  `parent_directory_id` bigint unsigned COMMENT 'FK: 부모 디렉터리 (Root=NULL)',
  `read_level` int NOT NULL DEFAULT 0 COMMENT '읽기 가능 레벨',
  `write_level` int NOT NULL DEFAULT 0 COMMENT '쓰기 가능 레벨',
  `name` varchar(100) NOT NULL COMMENT '디렉터리 이름',
  `depth_level` int NOT NULL DEFAULT 0 COMMENT '디렉터리 깊이 (Root=0)',
  `deleted_at` datetime,
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `virtual_directory_stats` (
  `directory_id` bigint unsigned PRIMARY KEY COMMENT 'PK & FK: 디렉터리와 1:1',
  `total_size` bigint NOT NULL DEFAULT 0 COMMENT '하위 포함 총 용량',
  `directory_count` int NOT NULL DEFAULT 0 COMMENT '하위 모든 디렉터리 개수',
  `file_count` int NOT NULL DEFAULT 0 COMMENT '하위 모든 파일 개수',
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `virtual_files` (
  `virtual_file_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `directory_id` bigint unsigned NOT NULL COMMENT 'FK: 소속 디렉터리',
  `real_file_id` bigint unsigned NOT NULL COMMENT 'FK: 실제 파일 메타데이터',
  `read_level` int NOT NULL DEFAULT 0 COMMENT '읽기 가능 레벨',
  `write_level` int NOT NULL DEFAULT 0 COMMENT '쓰기 가능 레벨',
  `name` varchar(100) NOT NULL COMMENT '파일 이름',
  `extension` varchar(50) COMMENT '파일 확장자',
  `deleted_at` datetime,
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `real_files` (
  `real_file_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `storage_uuid` varchar(32) UNIQUE NOT NULL COMMENT '파일이 저장된 이름',
  `file_hash` char(64) NOT NULL COMMENT 'SHA-256 Hash for Deduplication',
  `file_size` bigint NOT NULL COMMENT '파일 크기',
  `storage_path` varchar(255) NOT NULL COMMENT '파일 저장 위치',
  `storage_type` varchar(10) NOT NULL COMMENT 'LOCAL, S3, MINIO',
  `reference_count` int NOT NULL DEFAULT 0 COMMENT '참조 카운트',
  `deleted_at` datetime,
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `upload_sessions` (
  `session_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `file_permission_id` bigint unsigned NOT NULL COMMENT 'FK: 파일을 업로드하는 권한 키',
  `user_id` bigint unsigned NOT NULL COMMENT 'FK: 파일을 업로드하는 유저',
  `session_uuid` varchar(32) UNIQUE NOT NULL COMMENT '세션 구분용 uuid',
  `total_parts` int NOT NULL COMMENT '업로드 될 파츠 개수',
  `total_size` bigint NOT NULL COMMENT '업로드 될 용량',
  `upload_task_id` varchar(255) COMMENT '오브젝트 스토리지의 작업 ID',
  `local_path` varchar(255) COMMENT '로컬 서버에 저장시 임시 폴더 경로',
  `last_active_at` datetime NOT NULL DEFAULT (now()) COMMENT '마지막으로 전송된 시각',
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `upload_parts` (
  `part_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `session_id` bigint unsigned NOT NULL COMMENT 'FK: 파츠의 소속',
  `part_number` int NOT NULL COMMENT '파츠의 순서',
  `part_size` int NOT NULL COMMENT '파츠의 용량',
  `etag` varchar(100) COMMENT '오브젝트 스토리지용 반환 증명서',
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `share_links` (
  `share_link_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT 'FK: 생성자',
  `share_uuid` varchar(32) UNIQUE NOT NULL COMMENT '공유용 uuid',
  `link_type` varchar(15) NOT NULL COMMENT 'INVITE, FILE, GROUP',
  `expiration_date` datetime NOT NULL COMMENT '공유 기간',
  `password` varchar(255) COMMENT '없는 경우 NULL',
  `max_use_count` int NOT NULL COMMENT '사용 가능 횟수',
  `current_use_count` int NOT NULL DEFAULT 0 COMMENT '사용 횟수',
  `name` varchar(50) NOT NULL COMMENT '공유링크 이름',
  `deleted_at` datetime,
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `share_directories` (
  `share_directory_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `share_link_id` bigint unsigned NOT NULL COMMENT 'FK: 공유링크 id',
  `parent_directory_id` bigint unsigned COMMENT 'FK: 부모 디렉터리 (Root=NULL)',
  `name` varchar(100) NOT NULL COMMENT '이름',
  `depth_level` int NOT NULL DEFAULT 0 COMMENT '디렉터리 깊이',
  `deleted_at` datetime,
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `share_files` (
  `share_file_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `share_directory_id` bigint unsigned NOT NULL COMMENT 'FK: 소속 디렉터리',
  `virtual_file_id` bigint unsigned NOT NULL COMMENT 'FK: 공유된 가상 파일',
  `name` varchar(100) NOT NULL COMMENT '이름',
  `extension` varchar(50) COMMENT '확장자',
  `deleted_at` datetime,
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `group_invites` (
  `share_link_id` bigint unsigned PRIMARY KEY COMMENT 'PK & FK: share_links 1:1',
  `group_id` bigint unsigned NOT NULL COMMENT 'FK: 그룹id',
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `groups` (
  `group_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `file_permission_id` bigint unsigned NOT NULL COMMENT 'FK: 그룹의 파일 권한 키',
  `owner_id` bigint unsigned NOT NULL COMMENT 'FK: 그룹 소유자',
  `name` varchar(50) NOT NULL COMMENT '그룹 이름',
  `max_member_count` int NOT NULL COMMENT '가입할 수 인원수',
  `current_member_count` int NOT NULL DEFAULT 0 COMMENT '가입한 인원수',
  `deleted_at` datetime,
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `group_users` (
  `group_user_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT,
  `group_id` bigint unsigned NOT NULL COMMENT 'FK: 소속 그룹',
  `user_id` bigint unsigned NOT NULL COMMENT 'FK: 유저',
  `read_level` int NOT NULL DEFAULT 0 COMMENT '읽을 수 있는 레벨',
  `write_level` int NOT NULL DEFAULT 0 COMMENT '수정할 수 있는 레벨',
  `deleted_at` datetime,
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT (now()) COMMENT '수정 시각',
  `created_by` bigint unsigned NOT NULL COMMENT '생성자 ID (System=1)',
  `updated_by` bigint unsigned NOT NULL COMMENT '수정자 ID'
);

CREATE TABLE `group_user_permissions` (
  `group_user_id` bigint unsigned PRIMARY KEY COMMENT 'PK & FK: group_users 1:1',
  `can_manage_user` boolean NOT NULL DEFAULT false,
  `can_manage_file` boolean NOT NULL DEFAULT false,
  `created_at` datetime NOT NULL DEFAULT (now()),
  `updated_at` datetime NOT NULL DEFAULT (now()),
  `created_by` bigint unsigned NOT NULL,
  `updated_by` bigint unsigned NOT NULL
);

CREATE TABLE `batch_job_queues` (
  `batch_job_queue_id` bigint unsigned PRIMARY KEY AUTO_INCREMENT COMMENT '배치 작업 대기열id (대리키)',
  `job_type` varchar(50) NOT NULL COMMENT '작업 유형(통계/집계 등)',
  `target_table` varchar(64) NOT NULL COMMENT '대상 테이블명(FK 미적용)',
  `target_id` bigint unsigned NOT NULL COMMENT '대상 테이블의 작업 대상 id',
  `status` varchar(15) NOT NULL DEFAULT 'wait' COMMENT 'wait|in_progress|retry_wait|fail|success',
  `job_data` json NOT NULL COMMENT '작업 데이터(상세 파라미터)',
  `attempt_count` int NOT NULL DEFAULT 0 COMMENT '시도 횟수(현재까지 실행 시도된 횟수)',
  `max_attempts` int NOT NULL COMMENT '최대 실행 가능 횟수(재시도 허용 횟수 상한)',
  `next_run_at` datetime NOT NULL COMMENT '다음 실행 시각(이 시각 이후 실행 가능)',
  `created_at` datetime NOT NULL DEFAULT (now()) COMMENT '생성일',
  `finished_at` datetime COMMENT '완료일(미완료 시 NULL)',
  `updated_at` datetime NOT NULL DEFAULT (now()),
  `created_by` bigint unsigned NOT NULL COMMENT 'System or User',
  `updated_by` bigint unsigned NOT NULL COMMENT 'System'
);

CREATE INDEX `idx_capacity_allocations_active` ON `capacity_allocations` (`receiver_permission_id`, `deleted_at`, `expiration_date`, `allocated_size`);

CREATE INDEX `idx_virtual_directories_parent_name` ON `virtual_directories` (`file_permission_id`, `parent_directory_id`, `deleted_at`, `name`, `directory_id`);

CREATE INDEX `idx_virtual_directories_parent_updated` ON `virtual_directories` (`file_permission_id`, `parent_directory_id`, `deleted_at`, `updated_at`, `directory_id`);

CREATE INDEX `idx_virtual_files_directory_name` ON `virtual_files` (`directory_id`, `deleted_at`, `name`, `extension`, `virtual_file_id`);

CREATE INDEX `idx_virtual_files_directory_updated` ON `virtual_files` (`directory_id`, `deleted_at`, `updated_at`, `extension`, `virtual_file_id`);

CREATE INDEX `idx_real_files_hash_size` ON `real_files` (`file_hash`, `file_size`);

CREATE INDEX `idx_upload_parts_session_order` ON `upload_parts` (`session_id`, `part_number`);

CREATE INDEX `idx_share_directories_link_parent` ON `share_directories` (`share_link_id`, `deleted_at`, `parent_directory_id`, `name`, `share_directory_id`);

CREATE INDEX `idx_share_files_directory_name` ON `share_files` (`share_directory_id`, `deleted_at`, `name`, `share_file_id`);

CREATE INDEX `idx_batch_job_queues_polling` ON `batch_job_queues` (`status`, `next_run_at`);

ALTER TABLE `users` ADD FOREIGN KEY (`inviter_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `users` ADD FOREIGN KEY (`file_permission_id`) REFERENCES `file_permission_keys` (`file_permission_id`);

ALTER TABLE `user_permissions` ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `file_permission_keys` ADD FOREIGN KEY (`parent_permission_id`) REFERENCES `file_permission_keys` (`file_permission_id`);

ALTER TABLE `capacity_allocations` ADD FOREIGN KEY (`granter_allocation_id`) REFERENCES `capacity_allocations` (`allocation_id`);

ALTER TABLE `capacity_allocations` ADD FOREIGN KEY (`receiver_permission_id`) REFERENCES `file_permission_keys` (`file_permission_id`);

ALTER TABLE `capacity_allocations` ADD FOREIGN KEY (`giver_permission_id`) REFERENCES `file_permission_keys` (`file_permission_id`);

ALTER TABLE `virtual_directories` ADD FOREIGN KEY (`file_permission_id`) REFERENCES `file_permission_keys` (`file_permission_id`);

ALTER TABLE `virtual_directories` ADD FOREIGN KEY (`parent_directory_id`) REFERENCES `virtual_directories` (`directory_id`);

ALTER TABLE `virtual_directory_stats` ADD FOREIGN KEY (`directory_id`) REFERENCES `virtual_directories` (`directory_id`);

ALTER TABLE `virtual_files` ADD FOREIGN KEY (`directory_id`) REFERENCES `virtual_directories` (`directory_id`);

ALTER TABLE `virtual_files` ADD FOREIGN KEY (`real_file_id`) REFERENCES `real_files` (`real_file_id`);

ALTER TABLE `upload_sessions` ADD FOREIGN KEY (`file_permission_id`) REFERENCES `file_permission_keys` (`file_permission_id`);

ALTER TABLE `upload_sessions` ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `upload_parts` ADD FOREIGN KEY (`session_id`) REFERENCES `upload_sessions` (`session_id`);

ALTER TABLE `share_links` ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `share_directories` ADD FOREIGN KEY (`share_link_id`) REFERENCES `share_links` (`share_link_id`);

ALTER TABLE `share_directories` ADD FOREIGN KEY (`parent_directory_id`) REFERENCES `share_directories` (`share_directory_id`);

ALTER TABLE `share_files` ADD FOREIGN KEY (`share_directory_id`) REFERENCES `share_directories` (`share_directory_id`);

ALTER TABLE `share_files` ADD FOREIGN KEY (`virtual_file_id`) REFERENCES `virtual_files` (`virtual_file_id`);

ALTER TABLE `group_invites` ADD FOREIGN KEY (`share_link_id`) REFERENCES `share_links` (`share_link_id`);

ALTER TABLE `group_invites` ADD FOREIGN KEY (`group_id`) REFERENCES `groups` (`group_id`);

ALTER TABLE `groups` ADD FOREIGN KEY (`file_permission_id`) REFERENCES `file_permission_keys` (`file_permission_id`);

ALTER TABLE `groups` ADD FOREIGN KEY (`owner_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `group_users` ADD FOREIGN KEY (`group_id`) REFERENCES `groups` (`group_id`);

ALTER TABLE `group_users` ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `group_user_permissions` ADD FOREIGN KEY (`group_user_id`) REFERENCES `group_users` (`group_user_id`);
