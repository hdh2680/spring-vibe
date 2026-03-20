package springVibe.dev.users.youtubeComment.dto;

import java.time.OffsetDateTime;

public class YoutubeCommentItem {
    private String commentId;
    private String authorDisplayName;
    private String text;
    private Long likeCount;
    private OffsetDateTime publishedAt;
    // For UI rendering: formatted in KST as "yyyy-MM-dd HH:mm:ss"
    private String publishedAtView;

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public void setAuthorDisplayName(String authorDisplayName) {
        this.authorDisplayName = authorDisplayName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getPublishedAtView() {
        return publishedAtView;
    }

    public void setPublishedAtView(String publishedAtView) {
        this.publishedAtView = publishedAtView;
    }
}
