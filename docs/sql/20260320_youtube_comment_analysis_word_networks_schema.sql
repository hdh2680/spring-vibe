-- 유튜브 댓글 네트워크 분석(단어 공동출현 네트워크) 결과 테이블
-- - history_id 기준으로 최신 결과를 조회하고, 재분석 시 기존 결과 삭제 후 재적재한다.

CREATE TABLE IF NOT EXISTS `youtube_comment_analysis_word_networks` (
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

