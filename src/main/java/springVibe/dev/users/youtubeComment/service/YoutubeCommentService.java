package springVibe.dev.users.youtubeComment.service;

import springVibe.dev.users.youtubeComment.client.YoutubeDataApiClient;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentItem;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentPage;
import springVibe.dev.users.youtubeComment.dto.youtube.CommentThreadsResponse;
import springVibe.dev.users.youtubeComment.util.YoutubeUrlParser;
import springVibe.system.exception.BaseException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class YoutubeCommentService {
    private static final int DEFAULT_MAX_RESULTS = 100;

    private final YoutubeDataApiClient youtubeDataApiClient;

    public YoutubeCommentService(YoutubeDataApiClient youtubeDataApiClient) {
        this.youtubeDataApiClient = youtubeDataApiClient;
    }

    public YoutubeCommentPage collectCommentsByUrl(String inputUrl, String pageToken) {
        String videoId = YoutubeUrlParser.extractVideoId(inputUrl);
        if (videoId == null || videoId.isBlank()) {
            throw new BaseException("YOUTUBE_URL_INVALID", "유효한 유튜브 URL이 아닙니다(videoId 추출 실패).");
        }
        YoutubeCommentPage page = collectComments(videoId, pageToken);
        page.setInputUrl(inputUrl);
        return page;
    }

    public YoutubeCommentPage collectComments(String videoId, String pageToken) {
        CommentThreadsResponse res = youtubeDataApiClient.listCommentThreads(videoId, pageToken, DEFAULT_MAX_RESULTS);
        if (res == null) {
            throw new BaseException("YOUTUBE_API_EMPTY", "YouTube API 응답이 비어있습니다.");
        }

        List<YoutubeCommentItem> comments = new ArrayList<>();
        if (res.getItems() != null) {
            for (CommentThreadsResponse.Item it : res.getItems()) {
                if (it == null || it.getSnippet() == null || it.getSnippet().getTopLevelComment() == null) {
                    continue;
                }
                CommentThreadsResponse.TopLevelComment c = it.getSnippet().getTopLevelComment();
                CommentThreadsResponse.CommentSnippet s = c.getSnippet();
                if (s == null) {
                    continue;
                }

                YoutubeCommentItem item = new YoutubeCommentItem();
                item.setCommentId(c.getId());
                item.setAuthorDisplayName(s.getAuthorDisplayName());
                item.setText(s.getTextOriginal());
                item.setLikeCount(s.getLikeCount());

                if (s.getPublishedAt() != null && !s.getPublishedAt().isBlank()) {
                    try {
                        item.setPublishedAt(OffsetDateTime.parse(s.getPublishedAt()));
                    } catch (Exception ignored) {
                        // Best-effort parse.
                    }
                }

                comments.add(item);
            }
        }

        YoutubeCommentPage page = new YoutubeCommentPage();
        page.setVideoId(videoId);
        page.setComments(comments);
        page.setNextPageToken(res.getNextPageToken());
        page.setTotalResults(res.getPageInfo() == null ? null : res.getPageInfo().getTotalResults());
        return page;
    }
}

