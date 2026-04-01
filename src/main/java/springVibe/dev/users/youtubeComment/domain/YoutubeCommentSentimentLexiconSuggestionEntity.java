package springVibe.dev.users.youtubeComment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "youtube_comment_sentiment_lexicon_suggestions",
    indexes = {
        @Index(name = "idx_ycsls_history_status", columnList = "history_id,status"),
        @Index(name = "idx_ycsls_item", columnList = "sentiment_item_id")
    }
)
public class YoutubeCommentSentimentLexiconSuggestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "history_id", nullable = false)
    private Long historyId;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "sentiment_item_id", nullable = false)
    private Long sentimentItemId;

    @Column(name = "action", length = 16, nullable = false)
    private String action; // UPSERT / DELETE

    @Column(name = "term", length = 255, nullable = false)
    private String term;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "reason", length = 512, nullable = true)
    private String reason;

    @Column(name = "status", length = 16, nullable = false)
    private String status; // PENDING / APPLIED / REJECTED

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

    public Long getSentimentItemId() {
        return sentimentItemId;
    }

    public void setSentimentItemId(Long sentimentItemId) {
        this.sentimentItemId = sentimentItemId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

