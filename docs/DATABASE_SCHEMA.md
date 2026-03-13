# DATABASE_SCHEMA

- DBMS: MySQL
- Database: iamdb
- Generated at: 2026-03-13 11:16:27 +09:00
- Source: SHOW CREATE TABLE

## Tables

- `login_logs`
- `menus`
- `role_menus`
- `roles`
- `user_roles`
- `users`

## `login_logs`

```sql
CREATE TABLE `login_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '로그 ID',
  `user_id` bigint DEFAULT NULL COMMENT '회원 ID',
  `username` varchar(50) DEFAULT NULL COMMENT '로그인 시도 ID',
  `login_ip` varchar(50) DEFAULT NULL COMMENT '접속 IP 주소',
  `user_agent` varchar(255) DEFAULT NULL COMMENT '브라우저 정보',
  `login_result` varchar(20) DEFAULT NULL COMMENT '로그인 결과 (SUCCESS, FAIL)',
  `login_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '로그인 시도 시간',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `login_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='사용자 로그인 이력 로그 테이블'
```

## `menus`

```sql
CREATE TABLE `menus` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `menu_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `menu_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menus_menu_key` (`menu_key`),
  KEY `ix_menus_parent_sort` (`parent_id`,`sort_order`),
  CONSTRAINT `fk_menus_parent` FOREIGN KEY (`parent_id`) REFERENCES `menus` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
```

## `role_menus`

```sql
CREATE TABLE `role_menus` (
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  `can_access` tinyint(1) NOT NULL DEFAULT '1',
  `granted_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`role_id`,`menu_id`),
  KEY `ix_role_menus_menu_id` (`menu_id`),
  CONSTRAINT `fk_role_menus_menu` FOREIGN KEY (`menu_id`) REFERENCES `menus` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_menus_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
```

## `roles`

```sql
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '권한 고유 ID',
  `role_name` varchar(50) NOT NULL COMMENT '권한 이름 (ROLE_ADMIN, ROLE_USER)',
  `description` varchar(255) DEFAULT NULL COMMENT '권한 설명',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='시스템 권한 정보 테이블'
```

## `user_roles`

```sql
CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL COMMENT '회원 ID',
  `role_id` bigint NOT NULL COMMENT '권한 ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '권한 부여일',
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `user_roles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `user_roles_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='사용자와 권한 매핑 테이블'
```

## `users`

```sql
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '회원 고유 ID',
  `username` varchar(50) NOT NULL COMMENT '로그인 ID',
  `password` varchar(255) NOT NULL COMMENT '암호화된 비밀번호',
  `name` varchar(100) DEFAULT NULL COMMENT '회원 이름',
  `email` varchar(100) DEFAULT NULL COMMENT '이메일 주소',
  `phone` varchar(20) DEFAULT NULL COMMENT '전화번호',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '계정 상태 (ACTIVE, LOCK, WITHDRAW)',
  `login_fail_count` int DEFAULT '0' COMMENT '로그인 실패 횟수',
  `last_login_at` datetime DEFAULT NULL COMMENT '마지막 로그인 시간',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='회원 기본 정보 테이블'
```

