package springVibe.dev.users.youtubeComment.dto;

public class YoutubeCommentSentimentItemRow {
    private Long id;
    private String commentId;
    private String publishedAt;
    private String textClean;
    private String lexLabel;
    private int lexScore;
    private int matched;

    private String llmLabel;
    private String finalLabel;
    private int corrected;
    private String correctedReason;

    public YoutubeCommentSentimentItemRow() {
    }

    public YoutubeCommentSentimentItemRow(String commentId, String publishedAt, String textClean, String lexLabel, int lexScore, int matched) {
        this.commentId = commentId;
        this.publishedAt = publishedAt;
        this.textClean = textClean;
        this.lexLabel = lexLabel;
        this.lexScore = lexScore;
        this.matched = matched;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getLexScore() {
        return lexScore;
    }

    public void setLexScore(int lexScore) {
        this.lexScore = lexScore;
    }

    public int getMatched() {
        return matched;
    }

    public void setMatched(int matched) {
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

    public int getCorrected() {
        return corrected;
    }

    public void setCorrected(int corrected) {
        this.corrected = corrected;
    }

    public String getCorrectedReason() {
        return correctedReason;
    }

    public void setCorrectedReason(String correctedReason) {
        this.correctedReason = correctedReason;
    }
}
