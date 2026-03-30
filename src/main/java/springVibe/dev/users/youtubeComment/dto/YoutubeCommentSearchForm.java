package springVibe.dev.users.youtubeComment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class YoutubeCommentSearchForm {
    @NotBlank(message = "유튜브 URL을 입력해주세요.")
    private String url;

    private String pageToken;

    @Min(value = 1, message = "가져올 개수는 1 이상이어야 합니다.")
    @Max(value = 500, message = "가져올 개수는 최대 500개까지 가능합니다.")
    private Integer limit;

    @Size(max = 1024, message = "비고는 최대 1024자까지 입력할 수 있습니다.")
    private String remark;

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

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

