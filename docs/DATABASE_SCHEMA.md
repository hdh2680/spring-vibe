# DATABASE_SCHEMA

- DBMS: MySQL
- Database: springVibe
- Generated at: 2026-03-30 17:13:10 +09:00
- Source: SHOW CREATE TABLE (and manual migration draft for new/changed tables)

## Tables

- `login_logs`
- `menus`
- `role_menus`
- `roles`
- `user_roles`
- `users`
- `amazon_category`
- `amazon_product`
- `youtube_comment_analysis_histories`
- `youtube_comment_analysis_top_keywords`
- `youtube_comment_analysis_topic_groups`
- `youtube_comment_analysis_sentiments`
- `youtube_comment_analysis_word_networks`

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

## `amazon_category`

```sql
CREATE TABLE `amazon_category` (
  `id` bigint NOT NULL COMMENT '카테고리 ID (CSV 고정 ID)',

  `category_name` varchar(255) NOT NULL COMMENT '카테고리명(원문)',
  `category_name_ko` varchar(255) DEFAULT NULL COMMENT '카테고리명(한글)',

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Amazon 카테고리';
```

## `amazon_product`

```sql
CREATE TABLE `amazon_product` (
  `asin` varchar(32) NOT NULL COMMENT 'ASIN',

  `title` varchar(2000) DEFAULT NULL COMMENT '제품명(원문)',
  `product_name_ko` varchar(2000) DEFAULT NULL COMMENT '제품명(한글)',

  `img_url` varchar(2000) DEFAULT NULL COMMENT '이미지 URL',
  `product_url` varchar(2000) DEFAULT NULL COMMENT '상품 URL',

  `stars` double DEFAULT NULL COMMENT '별점',
  `reviews` int DEFAULT NULL COMMENT '리뷰 수',
  `price` double DEFAULT NULL COMMENT '가격',
  `list_price` double DEFAULT NULL COMMENT '정가',

  `category_id` bigint DEFAULT NULL COMMENT '카테고리 ID (amazon_category.id)',
  `is_best_seller` varchar(5) DEFAULT NULL COMMENT '베스트셀러 여부(True/False)',
  `bought_in_last_month` int DEFAULT NULL COMMENT '지난달 구매 수',

  PRIMARY KEY (`asin`),
  KEY `ix_amazon_product_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Amazon 상품';
```

## `youtube_comment_analysis_histories`

```sql
CREATE TABLE `youtube_comment_analysis_histories` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '이력 ID',

  `user_id` bigint DEFAULT NULL COMMENT '회원 ID',
  `video_url` varchar(2048) NOT NULL COMMENT '영상 URL',
  `video_title` varchar(512) DEFAULT NULL COMMENT '영상명',

  `original_file_path` varchar(1024) NOT NULL COMMENT '원본파일 저장 경로',
  `original_saved_at` datetime(6) NOT NULL COMMENT '원본파일 저장 일시',

  `preprocessed_file_path` varchar(1024) DEFAULT NULL COMMENT '전처리파일 저장 경로',
  `preprocessed_saved_at` datetime(6) DEFAULT NULL COMMENT '전처리 일시',

  `analysis_requested_at` datetime(6) DEFAULT NULL COMMENT '분석요청일시',
  `remark` varchar(1024) DEFAULT NULL COMMENT '비고',

  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일',

  PRIMARY KEY (`id`),
  KEY `ix_ycah_user_created` (`user_id`, `created_at`),
  CONSTRAINT `fk_ycah_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='유튜브 댓글 저장/분석 이력';
```

## `youtube_comment_analysis_top_keywords`

```sql
CREATE TABLE `youtube_comment_analysis_top_keywords` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '키워드 분석 결과 ID',

  `history_id` bigint NOT NULL COMMENT '이력 ID (youtube_comment_analysis_histories.id)',
  `user_id` bigint DEFAULT NULL COMMENT '회원 ID',

  `requested_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '분석 요청 일시',
  `params_json` json DEFAULT NULL COMMENT '분석 파라미터(JSON)',
  `result_json` json NOT NULL COMMENT 'Top N 키워드 결과(JSON)',

  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일',

  PRIMARY KEY (`id`),
  KEY `ix_ycatk_history_created` (`history_id`, `created_at`),
  KEY `ix_ycatk_user_created` (`user_id`, `created_at`),
  CONSTRAINT `fk_ycatk_history`
    FOREIGN KEY (`history_id`) REFERENCES `youtube_comment_analysis_histories` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ycatk_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='유튜브 댓글 키워드(Top N) 분석 결과';
```

## `youtube_comment_analysis_topic_groups`

```sql
CREATE TABLE `youtube_comment_analysis_topic_groups` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '주제별 묶음 분석 결과 ID',

  `history_id` bigint NOT NULL COMMENT '이력 ID (youtube_comment_analysis_histories.id)',
  `user_id` bigint DEFAULT NULL COMMENT '회원 ID',

  `requested_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '분석 요청 일시',
  `params_json` json DEFAULT NULL COMMENT '분석 파라미터(JSON)',
  `result_json` json NOT NULL COMMENT '주제별 묶음 결과(JSON)',

  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일',

  PRIMARY KEY (`id`),
  KEY `ix_ycatg_history_created` (`history_id`, `created_at`),
  KEY `ix_ycatg_user_created` (`user_id`, `created_at`),
  CONSTRAINT `fk_ycatg_history`
    FOREIGN KEY (`history_id`) REFERENCES `youtube_comment_analysis_histories` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ycatg_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='유튜브 댓글 주제별 묶음 분석 결과';
```

## `youtube_comment_analysis_sentiments`

```sql
CREATE TABLE `youtube_comment_analysis_sentiments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '감정분석 결과 ID',

  `history_id` bigint NOT NULL COMMENT '이력 ID (youtube_comment_analysis_histories.id)',
  `user_id` bigint DEFAULT NULL COMMENT '회원 ID',

  `requested_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '분석 요청 일시',
  `params_json` json DEFAULT NULL COMMENT '분석 파라미터(JSON)',
  `result_json` json NOT NULL COMMENT '감정분석 결과(JSON)',

  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일',

  PRIMARY KEY (`id`),
  KEY `ix_ycas_history_created` (`history_id`, `created_at`),
  KEY `ix_ycas_user_created` (`user_id`, `created_at`),
  CONSTRAINT `fk_ycas_history`
    FOREIGN KEY (`history_id`) REFERENCES `youtube_comment_analysis_histories` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ycas_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='유튜브 댓글 감정분석(사전기반) 결과';
```

## `youtube_comment_analysis_word_networks`

```sql
CREATE TABLE `youtube_comment_analysis_word_networks` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '네트워크 분석 결과 ID',

  `history_id` bigint NOT NULL COMMENT '이력 ID (youtube_comment_analysis_histories.id)',
  `user_id` bigint DEFAULT NULL COMMENT '회원 ID',

  `requested_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '분석 요청 일시',
  `params_json` json DEFAULT NULL COMMENT '분석 파라미터(JSON)',
  `result_json` json NOT NULL COMMENT '네트워크 분석 결과(JSON)',

  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일',

  PRIMARY KEY (`id`),
  KEY `ix_ycawn_history_created` (`history_id`, `created_at`),
  KEY `ix_ycawn_user_created` (`user_id`, `created_at`),
  CONSTRAINT `fk_ycawn_history`
    FOREIGN KEY (`history_id`) REFERENCES `youtube_comment_analysis_histories` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ycawn_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='유튜브 댓글 네트워크 분석(단어 공동출현) 결과';
```
