package springVibe.dev.users.youtubeComment.domain;

import java.time.LocalDateTime;

/**
 * PRD: 저장 버튼 클릭 시 youtube_comment_analysis_histories 에 저장되는 이력.
 * 현재 MVP에서는 videoUrl/originalFilePath/originalSavedAt(+userId)만 사용한다.
 */
public class YoutubeCommentAnalysisHistory {
    private Long id;
    private Long userId;
    private String videoUrl;
    private String originalFilePath;
    private LocalDateTime originalSavedAt;
    private String preprocessedFilePath;
    private LocalDateTime preprocessedSavedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public void setOriginalFilePath(String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }

    public LocalDateTime getOriginalSavedAt() {
        return originalSavedAt;
    }

    public void setOriginalSavedAt(LocalDateTime originalSavedAt) {
        this.originalSavedAt = originalSavedAt;
    }

    public String getPreprocessedFilePath() {
        return preprocessedFilePath;
    }

    public void setPreprocessedFilePath(String preprocessedFilePath) {
        this.preprocessedFilePath = preprocessedFilePath;
    }

    public LocalDateTime getPreprocessedSavedAt() {
        return preprocessedSavedAt;
    }

    public void setPreprocessedSavedAt(LocalDateTime preprocessedSavedAt) {
        this.preprocessedSavedAt = preprocessedSavedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
