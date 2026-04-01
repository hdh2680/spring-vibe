package springVibe.dev.users.youtubeComment.dto;

public class YoutubeCommentLexiconSuggestionRow {
    private Long id;
    private Long sentimentItemId;
    private String action;
    private String term;
    private int score;
    private String reason;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
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
}

