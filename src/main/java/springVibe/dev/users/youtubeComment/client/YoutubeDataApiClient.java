package springVibe.dev.users.youtubeComment.client;

import springVibe.dev.users.youtubeComment.config.YoutubeIntegrationProperties;
import springVibe.dev.users.youtubeComment.dto.youtube.CommentThreadsResponse;
import springVibe.dev.users.youtubeComment.dto.youtube.VideosResponse;
import springVibe.system.exception.BaseException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * YouTube Data API v3 client (MVP: commentThreads.list + videos.list(snippet.title)).
 */
@Component
public class YoutubeDataApiClient {
    private static final String YOUTUBE_API_BASE_URL = "https://www.googleapis.com";
    private static final int MIN_MAX_RESULTS = 1;
    private static final int MAX_MAX_RESULTS = 100;

    private final YoutubeIntegrationProperties properties;
    private final RestClient restClient;

    public YoutubeDataApiClient(YoutubeIntegrationProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(YOUTUBE_API_BASE_URL).build();
    }

    public CommentThreadsResponse listCommentThreads(String videoId, String pageToken, int maxResults) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BaseException("YOUTUBE_API_KEY_MISSING", "YouTube API Key가 설정되어 있지 않습니다(application.yml).");
        }
        if (videoId == null || videoId.isBlank()) {
            throw new BaseException("YOUTUBE_VIDEO_ID_REQUIRED", "videoId는 필수입니다.");
        }
        if (maxResults < MIN_MAX_RESULTS || maxResults > MAX_MAX_RESULTS) {
            throw new BaseException("YOUTUBE_MAX_RESULTS_INVALID", "maxResults는 " + MIN_MAX_RESULTS + "~" + MAX_MAX_RESULTS + " 범위여야 합니다.");
        }

        try {
            return restClient
                .get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                        .path("/youtube/v3/commentThreads")
                        .queryParam("part", "snippet")
                        .queryParam("videoId", videoId)
                        .queryParam("maxResults", maxResults)
                        .queryParam("order", "time")
                        .queryParam("textFormat", "plainText")
                        .queryParam("key", apiKey);
                    if (pageToken != null && !pageToken.isBlank()) {
                        b = b.queryParam("pageToken", pageToken);
                    }
                    return b.build();
                })
                .retrieve()
                .body(CommentThreadsResponse.class);
        } catch (RestClientResponseException e) {
            HttpStatusCode status = HttpStatusCode.valueOf(e.getStatusCode().value());
            String msg = "YouTube API 호출 실패(status=" + status.value() + ")";
            throw new BaseException("YOUTUBE_API_ERROR", msg, e);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_API_ERROR", "YouTube API 호출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * @return title or null (not found / empty)
     */
    public String getVideoTitle(String videoId) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BaseException("YOUTUBE_API_KEY_MISSING", "YouTube API Key가 설정되어 있지 않습니다(application.yml).");
        }
        if (videoId == null || videoId.isBlank()) {
            throw new BaseException("YOUTUBE_VIDEO_ID_REQUIRED", "videoId는 필수입니다.");
        }

        try {
            VideosResponse res = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/youtube/v3/videos")
                    .queryParam("part", "snippet")
                    .queryParam("id", videoId)
                    .queryParam("key", apiKey)
                    .build()
                )
                .retrieve()
                .body(VideosResponse.class);

            if (res == null || res.getItems() == null || res.getItems().isEmpty()) {
                return null;
            }
            VideosResponse.Item it = res.getItems().get(0);
            String title = it == null || it.getSnippet() == null ? null : it.getSnippet().getTitle();
            if (title == null || title.isBlank()) {
                return null;
            }
            return title;
        } catch (RestClientResponseException e) {
            HttpStatusCode status = HttpStatusCode.valueOf(e.getStatusCode().value());
            String msg = "YouTube API 호출 실패(status=" + status.value() + ")";
            throw new BaseException("YOUTUBE_API_ERROR", msg, e);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_API_ERROR", "YouTube API 호출 중 오류가 발생했습니다.", e);
        }
    }
}

