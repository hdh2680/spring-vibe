-- YouTube comment analysis: sentiment results (2026-03-20)
-- NOTE: Review on a staging DB first. This is a manual migration (no Flyway/Liquibase in this repo).

CREATE TABLE IF NOT EXISTS `youtube_comment_analysis_sentiments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Sentiment analysis result ID',

  `history_id` bigint NOT NULL COMMENT 'History ID (youtube_comment_analysis_histories.id)',
  `user_id` bigint DEFAULT NULL COMMENT 'User ID',

  `requested_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Analysis requested at',
  `params_json` json DEFAULT NULL COMMENT 'Analysis parameters (JSON)',
  `result_json` json NOT NULL COMMENT 'Sentiment results (JSON)',

  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Created at',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Updated at',

  PRIMARY KEY (`id`),
  KEY `ix_ycas_history_created` (`history_id`, `created_at`),
  KEY `ix_ycas_user_created` (`user_id`, `created_at`),
  CONSTRAINT `fk_ycas_history`
    FOREIGN KEY (`history_id`) REFERENCES `youtube_comment_analysis_histories` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ycas_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='YouTube comment sentiment analysis results (lexicon-based)';

