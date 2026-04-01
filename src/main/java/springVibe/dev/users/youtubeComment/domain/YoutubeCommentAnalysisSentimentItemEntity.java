package springVibe.dev.users.youtubeComment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "youtube_comment_analysis_sentiment_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ycas_history_comment", columnNames = {"history_id", "comment_id"})
    },
    indexes = {
        @Index(name = "idx_ycas_history_user", columnList = "history_id,user_id"),
        @Index(name = "idx_ycas_history_final", columnList = "history_id,final_label"),
        @Index(name = "idx_ycas_history_lex", columnList = "history_id,lex_label")
    }
)
public class YoutubeCommentAnalysisSentimentItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "history_id", nullable = false)
    private Long historyId;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "comment_id", length = 128, nullable = false)
    private String commentId;

    @Column(name = "published_at", length = 64, nullable = true)
    private String publishedAt;

    @Lob
    @Column(name = "text_clean", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String textClean;

    @Column(name = "lex_label", length = 16, nullable = false)
    private String lexLabel;

    @Column(name = "lex_score", nullable = false)
    private Integer lexScore;

    @Column(name = "matched", nullable = false)
    private Integer matched;

    @Column(name = "llm_label", length = 16, nullable = true)
    private String llmLabel;

    @Column(name = "final_label", length = 16, nullable = true)
    private String finalLabel;

    @Column(name = "corrected", nullable = false)
    private boolean corrected;

    @Column(name = "corrected_reason", length = 512, nullable = true)
    private String correctedReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getTextClean() {
        return textClean;
    }

    public void setTextClean(String textClean) {
        this.textClean = textClean;
    }

    public String getLexLabel() {
        return lexLabel;
    }

    public void setLexLabel(String lexLabel) {
        this.lexLabel = lexLabel;
    }

    public Integer getLexScore() {
        return lexScore;
    }

    public void setLexScore(Integer lexScore) {
        this.lexScore = lexScore;
    }

    public Integer getMatched() {
        return matched;
    }

    public void setMatched(Integer matched) {
        this.matched = matched;
    }

    public String getLlmLabel() {
        return llmLabel;
    }

    public void setLlmLabel(String llmLabel) {
        this.llmLabel = llmLabel;
    }

    public String getFinalLabel() {
        return finalLabel;
    }

    public void setFinalLabel(String finalLabel) {
        this.finalLabel = finalLabel;
    }

    public boolean isCorrected() {
        return corrected;
    }

    public void setCorrected(boolean corrected) {
        this.corrected = corrected;
    }

    public String getCorrectedReason() {
        return correctedReason;
    }

    public void setCorrectedReason(String correctedReason) {
        this.correctedReason = correctedReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
