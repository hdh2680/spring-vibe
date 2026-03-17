package springVibe.dev.users.youtubeComment.dto;

import jakarta.validation.constraints.NotBlank;

public class YoutubeCommentSearchForm {
    @NotBlank(message = "유튜브 URL을 입력해주세요.")
    private String url;

    private String pageToken;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPageToken() {
        return pageToken;
    }

    public void setPageToken(String pageToken) {
        this.pageToken = pageToken;
    }
}

