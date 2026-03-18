package springVibe.dev.users.youtubeComment.dto;

import java.util.List;

public class YoutubeCommentPage {
    private String inputUrl;
    private String videoId;
    private Integer totalResults;
    private String nextPageToken;
    private Integer collectedCount;
    private Integer requestedLimit;
    private List<YoutubeCommentItem> comments;

    public String getInputUrl() {
        return inputUrl;
    }

    public void setInputUrl(String inputUrl) {
        this.inputUrl = inputUrl;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    public Integer getCollectedCount() {
        return collectedCount;
    }

    public void setCollectedCount(Integer collectedCount) {
        this.collectedCount = collectedCount;
    }

    public Integer getRequestedLimit() {
        return requestedLimit;
    }

    public void setRequestedLimit(Integer requestedLimit) {
        this.requestedLimit = requestedLimit;
    }

    public List<YoutubeCommentItem> getComments() {
        return comments;
    }

    public void setComments(List<YoutubeCommentItem> comments) {
        this.comments = comments;
    }
}
