package springVibe.dev.users.youtubeComment.service;

import springVibe.dev.users.youtubeComment.client.YoutubeDataApiClient;
import springVibe.dev.users.youtubeComment.mapper.YoutubeCommentAnalysisHistoryMapper;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentPage;
import springVibe.dev.users.youtubeComment.dto.youtube.CommentThreadsResponse;
import springVibe.system.storage.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YoutubeCommentServiceTest {
    @Mock
    private YoutubeDataApiClient youtubeDataApiClient;

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private YoutubeCommentAnalysisHistoryMapper youtubeCommentAnalysisHistoryMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private YoutubeCommentService service;

    @Test
    void collectComments_paginatesUntilLimitAndReturnsNextToken() {
        String videoId = "video1";

        when(youtubeDataApiClient.listCommentThreads(videoId, null, 100))
            .thenReturn(responseWithItems(100, "t2", 999));
        when(youtubeDataApiClient.listCommentThreads(videoId, "t2", 70))
            .thenReturn(responseWithItems(70, "t3", 999));

        YoutubeCommentPage page = service.collectComments(videoId, null, 170);

        assertEquals(170, page.getComments().size());
        assertEquals(170, page.getCollectedCount());
        assertEquals(170, page.getRequestedLimit());
        assertEquals("t3", page.getNextPageToken());
        assertEquals(999, page.getTotalResults());

        verify(youtubeDataApiClient).listCommentThreads(videoId, null, 100);
        verify(youtubeDataApiClient).listCommentThreads(videoId, "t2", 70);
    }

    @Test
    void exportAllCommentsByUrlAsJsonl_writesAllPages(@TempDir Path tempDir) throws Exception {
        String videoId = "video1";
        String url = "https://www.youtube.com/watch?v=" + videoId;

        when(storageProperties.getAttachmentsDir()).thenReturn(tempDir.toString());
        when(youtubeDataApiClient.listCommentThreads(videoId, null, 100))
            .thenReturn(responseWithItems(2, "t2", 2));
        when(youtubeDataApiClient.listCommentThreads(videoId, "t2", 100))
            .thenReturn(responseWithItems(1, null, 3));

        Path jsonl = service.exportAllCommentsByUrlAsJsonl(url);

        assertTrue(Files.exists(jsonl));
        List<String> lines = Files.readAllLines(jsonl, StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertTrue(Pattern.matches(".*\\\\youtubeComment\\\\\\d{8}_\\d{6}\\.jsonl$", jsonl.toString()));
    }

    @Test
    void exportAllCommentsByUrlAsJsonlAndSaveHistory_savesHistory(@TempDir Path tempDir) throws Exception {
        String videoId = "video1";
        String url = "https://www.youtube.com/watch?v=" + videoId;

        when(storageProperties.getAttachmentsDir()).thenReturn(tempDir.toString());
        when(youtubeDataApiClient.listCommentThreads(videoId, null, 100))
            .thenReturn(responseWithItems(1, null, 1));

        Path jsonl = service.exportAllCommentsByUrlAsJsonlAndSaveHistory(url, 123L);

        ArgumentCaptor<springVibe.dev.users.youtubeComment.domain.YoutubeCommentAnalysisHistory> captor =
            ArgumentCaptor.forClass(springVibe.dev.users.youtubeComment.domain.YoutubeCommentAnalysisHistory.class);
        verify(youtubeCommentAnalysisHistoryMapper).insert(captor.capture());

        var saved = captor.getValue();
        assertEquals(123L, saved.getUserId());
        assertEquals(url, saved.getVideoUrl());
        assertEquals(jsonl.toString(), saved.getOriginalFilePath());
        assertNotNull(saved.getOriginalSavedAt());
    }

    private static CommentThreadsResponse responseWithItems(int count, String nextPageToken, Integer totalResults) {
        CommentThreadsResponse res = new CommentThreadsResponse();
        res.setNextPageToken(nextPageToken);

        CommentThreadsResponse.PageInfo pageInfo = new CommentThreadsResponse.PageInfo();
        pageInfo.setTotalResults(totalResults);
        pageInfo.setResultsPerPage(count);
        res.setPageInfo(pageInfo);

        List<CommentThreadsResponse.Item> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(item("c" + i, "author" + i, "text" + i));
        }
        res.setItems(items);
        return res;
    }

    private static CommentThreadsResponse.Item item(String commentId, String author, String text) {
        CommentThreadsResponse.CommentSnippet s = new CommentThreadsResponse.CommentSnippet();
        s.setAuthorDisplayName(author);
        s.setTextOriginal(text);
        s.setLikeCount(0L);
        s.setPublishedAt("2020-01-01T00:00:00Z");

        CommentThreadsResponse.TopLevelComment c = new CommentThreadsResponse.TopLevelComment();
        c.setId(commentId);
        c.setSnippet(s);

        CommentThreadsResponse.Snippet snippet = new CommentThreadsResponse.Snippet();
        snippet.setTopLevelComment(c);

        CommentThreadsResponse.Item it = new CommentThreadsResponse.Item();
        it.setId("thread-" + commentId);
        it.setSnippet(snippet);
        return it;
    }
}
