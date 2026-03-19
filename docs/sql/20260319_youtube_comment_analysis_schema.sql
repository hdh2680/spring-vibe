-- YouTube comment analysis schema changes (2026-03-19)
-- NOTE: Review on a staging DB first. This is a manual migration (no Flyway/Liquibase in this repo).

-- 1) Histories: remove legacy analysis1~5 columns, add analysis request timestamp.
ALTER TABLE youtube_comment_analysis_histories
  DROP COLUMN analysis1_type,
  DROP COLUMN analysis1_file_path,
  DROP COLUMN analysis1_saved_at,
  DROP COLUMN analysis2_type,
  DROP COLUMN analysis2_file_path,
  DROP COLUMN analysis2_saved_at,
  DROP COLUMN analysis3_type,
  DROP COLUMN analysis3_file_path,
  DROP COLUMN analysis3_saved_at,
  DROP COLUMN analysis4_type,
  DROP COLUMN analysis4_file_path,
  DROP COLUMN analysis4_saved_at,
  DROP COLUMN analysis5_type,
  DROP COLUMN analysis5_file_path,
  DROP COLUMN analysis5_saved_at,
  ADD COLUMN analysis_requested_at datetime(6) DEFAULT NULL COMMENT '분석요청일시' AFTER preprocessed_saved_at;

-- 2) Top-N keyword analysis results (per history).
CREATE TABLE IF NOT EXISTS youtube_comment_analysis_top_keywords (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '키워드 분석 결과 ID',

  history_id bigint NOT NULL COMMENT '이력 ID (youtube_comment_analysis_histories.id)',
  user_id bigint DEFAULT NULL COMMENT '회원 ID',

  requested_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '분석 요청 일시',
  params_json json DEFAULT NULL COMMENT '분석 파라미터(JSON)',
  result_json json NOT NULL COMMENT 'Top N 키워드 결과(JSON)',

  created_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일',
  updated_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일',

  PRIMARY KEY (id),
  KEY ix_ycatk_history_created (history_id, created_at),
  KEY ix_ycatk_user_created (user_id, created_at),
  CONSTRAINT fk_ycatk_history
    FOREIGN KEY (history_id) REFERENCES youtube_comment_analysis_histories (id) ON DELETE CASCADE,
  CONSTRAINT fk_ycatk_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='유튜브 댓글 키워드(Top N) 분석 결과';

-- 3) Topic grouping analysis results (per history).
CREATE TABLE IF NOT EXISTS youtube_comment_analysis_topic_groups (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '주제별 묶음 분석 결과 ID',

  history_id bigint NOT NULL COMMENT '이력 ID (youtube_comment_analysis_histories.id)',
  user_id bigint DEFAULT NULL COMMENT '회원 ID',

  requested_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '분석 요청 일시',
  params_json json DEFAULT NULL COMMENT '분석 파라미터(JSON)',
  result_json json NOT NULL COMMENT '주제별 묶음 결과(JSON)',

  created_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일',
  updated_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일',

  PRIMARY KEY (id),
  KEY ix_ycatg_history_created (history_id, created_at),
  KEY ix_ycatg_user_created (user_id, created_at),
  CONSTRAINT fk_ycatg_history
    FOREIGN KEY (history_id) REFERENCES youtube_comment_analysis_histories (id) ON DELETE CASCADE,
  CONSTRAINT fk_ycatg_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='유튜브 댓글 주제별 묶음 분석 결과';

