package springVibe.dev.users.youtubeComment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * This project currently relies on Hibernate ddl-auto=update without migrations.
 * Hibernate created youtube_comment_analysis_sentiment_items.text_clean as TINYTEXT (255 bytes),
 * which frequently fails when inserting real comment text. Also, user_id may be null depending on auth flow.
 *
 * Apply minimal, idempotent DDL fixes at startup.
 */
@Component
public class YoutubeCommentSentimentItemsSchemaFixer {
    private static final Logger log = LoggerFactory.getLogger(YoutubeCommentSentimentItemsSchemaFixer.class);

    private final JdbcTemplate jdbcTemplate;

    public YoutubeCommentSentimentItemsSchemaFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void fixSchema() {
        try {
            // Ensure table exists
            Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                "youtube_comment_analysis_sentiment_items"
            );
            if (exists == null || exists <= 0) {
                return;
            }

            fixUserIdNullable();
            fixTextCleanType();
        } catch (Exception e) {
            // Never block app startup due to best-effort schema fix.
            log.warn("Failed to auto-fix schema for youtube_comment_analysis_sentiment_items: {}", e.getMessage());
        }
    }

    private void fixUserIdNullable() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT IS_NULLABLE, DATA_TYPE, COLUMN_TYPE " +
                    "FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                "youtube_comment_analysis_sentiment_items",
                "user_id"
            );
            if (rows.isEmpty()) return;
            String isNullable = String.valueOf(rows.get(0).get("IS_NULLABLE"));
            if ("NO".equalsIgnoreCase(isNullable)) {
                jdbcTemplate.execute("ALTER TABLE youtube_comment_analysis_sentiment_items MODIFY user_id BIGINT NULL");
                log.info("Altered youtube_comment_analysis_sentiment_items.user_id to NULLABLE");
            }
        } catch (Exception e) {
            log.warn("Failed to alter user_id nullable: {}", e.getMessage());
        }
    }

    private void fixTextCleanType() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DATA_TYPE, COLUMN_TYPE " +
                    "FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                "youtube_comment_analysis_sentiment_items",
                "text_clean"
            );
            if (rows.isEmpty()) return;
            String dataType = String.valueOf(rows.get(0).get("DATA_TYPE"));
            if ("tinytext".equalsIgnoreCase(dataType)) {
                jdbcTemplate.execute("ALTER TABLE youtube_comment_analysis_sentiment_items MODIFY text_clean MEDIUMTEXT NOT NULL");
                log.info("Altered youtube_comment_analysis_sentiment_items.text_clean to MEDIUMTEXT");
            }
        } catch (Exception e) {
            log.warn("Failed to alter text_clean type: {}", e.getMessage());
        }
    }
}

