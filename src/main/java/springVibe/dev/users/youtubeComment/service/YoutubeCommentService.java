package springVibe.dev.users.youtubeComment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import springVibe.dev.users.youtubeComment.client.YoutubeDataApiClient;
import springVibe.dev.users.youtubeComment.domain.YoutubeCommentAnalysisHistory;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentItem;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentPage;
import springVibe.dev.users.youtubeComment.dto.youtube.CommentThreadsResponse;
import springVibe.dev.users.youtubeComment.mapper.YoutubeCommentAnalysisHistoryMapper;
import springVibe.dev.users.youtubeComment.util.YoutubeUrlParser;
import springVibe.system.storage.StorageProperties;
import springVibe.system.exception.BaseException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class YoutubeCommentService {
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter EXPORT_DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@[\\p{L}\\p{N}_.]{1,50}");
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#[\\p{L}\\p{N}_]{1,50}");
    private static final Pattern EMOJI_SURROGATE_PATTERN = Pattern.compile("[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]");
    private static final Pattern REPEAT_LAUGH_PATTERN = Pattern.compile("([ㅋㅎ])\\1{2,}");
    private static final Pattern REPEAT_CRY_PATTERN = Pattern.compile("([ㅠㅜ])\\1{2,}");

    private final YoutubeDataApiClient youtubeDataApiClient;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper;
    private final YoutubeCommentAnalysisHistoryMapper youtubeCommentAnalysisHistoryMapper;

    public YoutubeCommentService(
        YoutubeDataApiClient youtubeDataApiClient,
        StorageProperties storageProperties,
        ObjectMapper objectMapper,
        YoutubeCommentAnalysisHistoryMapper youtubeCommentAnalysisHistoryMapper
    ) {
        this.youtubeDataApiClient = youtubeDataApiClient;
        this.storageProperties = storageProperties;
        this.objectMapper = objectMapper;
        this.youtubeCommentAnalysisHistoryMapper = youtubeCommentAnalysisHistoryMapper;
    }

    public YoutubeCommentPage collectCommentsByUrl(String inputUrl, String pageToken) {
        return collectCommentsByUrl(inputUrl, pageToken, null);
    }

    public YoutubeCommentPage collectCommentsByUrl(String inputUrl, String pageToken, Integer limit) {
        String videoId = YoutubeUrlParser.extractVideoId(inputUrl);
        if (videoId == null || videoId.isBlank()) {
            throw new BaseException("YOUTUBE_URL_INVALID", "유효한 유튜브 URL이 아닙니다(videoId 추출 실패).");
        }
        YoutubeCommentPage page = collectComments(videoId, pageToken, limit);
        page.setInputUrl(inputUrl);
        return page;
    }

    public YoutubeCommentPage collectComments(String videoId, String pageToken) {
        return collectComments(videoId, pageToken, null);
    }

    public YoutubeCommentPage collectComments(String videoId, String pageToken, Integer limit) {
        int requestedLimit = normalizeLimit(limit);
        String currentPageToken = normalizePageToken(pageToken);

        List<YoutubeCommentItem> comments = new ArrayList<>(Math.min(requestedLimit, DEFAULT_PAGE_SIZE));
        String nextPageToken = null;
        Integer totalResults = null;

        while (comments.size() < requestedLimit) {
            int remaining = requestedLimit - comments.size();
            int pageSize = Math.min(MAX_PAGE_SIZE, remaining);
            int beforeSize = comments.size();

            CommentThreadsResponse res = youtubeDataApiClient.listCommentThreads(videoId, currentPageToken, pageSize);
            if (res == null) {
                throw new BaseException("YOUTUBE_API_EMPTY", "YouTube API 응답이 비어있습니다.");
            }

            if (totalResults == null) {
                totalResults = res.getPageInfo() == null ? null : res.getPageInfo().getTotalResults();
            }

            if (res.getItems() != null) {
                for (CommentThreadsResponse.Item it : res.getItems()) {
                    if (comments.size() >= requestedLimit) {
                        break;
                    }
                    YoutubeCommentItem mapped = mapToYoutubeCommentItem(it);
                    if (mapped != null) {
                        comments.add(mapped);
                    }
                }
            }

            nextPageToken = res.getNextPageToken();
            if (comments.size() == beforeSize) {
                // Avoid infinite loop if API returns empty/invalid items with a nextPageToken.
                break;
            }
            if (nextPageToken == null || nextPageToken.isBlank()) {
                break;
            }
            currentPageToken = nextPageToken;
        }

        YoutubeCommentPage page = new YoutubeCommentPage();
        page.setVideoId(videoId);
        page.setComments(comments);
        page.setNextPageToken(nextPageToken);
        page.setTotalResults(totalResults);
        page.setCollectedCount(comments.size());
        page.setRequestedLimit(requestedLimit);
        return page;
    }

    private static YoutubeCommentItem mapToYoutubeCommentItem(CommentThreadsResponse.Item it) {
        if (it == null || it.getSnippet() == null || it.getSnippet().getTopLevelComment() == null) {
            return null;
        }
        CommentThreadsResponse.TopLevelComment c = it.getSnippet().getTopLevelComment();
        CommentThreadsResponse.CommentSnippet s = c.getSnippet();
        if (s == null) {
            return null;
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

        return item;
    }

    private static int normalizeLimit(Integer limit) {
        int v = (limit == null ? DEFAULT_LIMIT : limit);
        if (v < 1) {
            v = DEFAULT_LIMIT;
        }
        return Math.min(v, MAX_LIMIT);
    }

    private static String normalizePageToken(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return null;
        }
        return pageToken;
    }

    public Path exportAllCommentsByUrlAsJsonl(String inputUrl) {
        String videoId = YoutubeUrlParser.extractVideoId(inputUrl);
        if (videoId == null || videoId.isBlank()) {
            throw new BaseException("YOUTUBE_URL_INVALID", "유효한 유튜브 URL이 아닙니다(videoId 추출 실패).");
        }

        Path jsonlPath = resolveExportPath();
        try {
            Files.createDirectories(jsonlPath.getParent());
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_EXPORT_DIR_CREATE_FAILED", "댓글 저장 폴더 생성에 실패했습니다.", e);
        }

        String pageToken = null;
        int written = 0;

        try (BufferedWriter w = Files.newBufferedWriter(jsonlPath, StandardCharsets.UTF_8)) {
            while (true) {
                int beforeWritten = written;

                CommentThreadsResponse res = youtubeDataApiClient.listCommentThreads(videoId, pageToken, MAX_PAGE_SIZE);
                if (res == null) {
                    throw new BaseException("YOUTUBE_API_EMPTY", "YouTube API 응답이 비어있습니다.");
                }

                if (res.getItems() != null) {
                    for (CommentThreadsResponse.Item it : res.getItems()) {
                        YoutubeCommentJsonlRow row = mapToJsonlRow(it);
                        if (row == null) {
                            continue;
                        }
                        w.write(objectMapper.writeValueAsString(row));
                        w.newLine();
                        written++;
                    }
                }

                String next = res.getNextPageToken();
                if (written == beforeWritten) {
                    // Avoid infinite loop if API returns empty/invalid items with a nextPageToken.
                    break;
                }
                if (next == null || next.isBlank()) {
                    break;
                }
                pageToken = next;
            }
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_EXPORT_FAILED", "댓글 JSONL 저장 중 오류가 발생했습니다.", e);
        }

        return jsonlPath;
    }

    /**
     * PRD: 저장 버튼 클릭 시 JSONL 파일을 저장하고, 저장 정보를 youtube_comment_analysis_histories 에 남긴다.
     */
    public Path exportAllCommentsByUrlAsJsonlAndSaveHistory(String inputUrl, Long userId) {
        Path jsonlPath = exportAllCommentsByUrlAsJsonl(inputUrl);

        YoutubeCommentAnalysisHistory history = new YoutubeCommentAnalysisHistory();
        history.setUserId(userId);
        history.setVideoUrl(inputUrl);
        history.setOriginalFilePath(jsonlPath.toString());
        history.setOriginalSavedAt(LocalDateTime.now(DEFAULT_ZONE_ID));

        try {
            youtubeCommentAnalysisHistoryMapper.insert(history);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_HISTORY_SAVE_FAILED", "저장 이력을 DB에 남기지 못했습니다.", e);
        }

        return jsonlPath;
    }

    public List<YoutubeCommentAnalysisHistory> listHistories(Long userId) {
        try {
            return youtubeCommentAnalysisHistoryMapper.selectList(userId);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_HISTORY_LIST_FAILED", "저장 이력 목록 조회에 실패했습니다.", e);
        }
    }

    public YoutubeCommentAnalysisHistory findHistoryOrThrow(Long id, Long userId) {
        if (id == null) {
            throw new BaseException("YOUTUBE_HISTORY_ID_REQUIRED", "이력 ID가 필요합니다.");
        }

        YoutubeCommentAnalysisHistory history;
        try {
            history = youtubeCommentAnalysisHistoryMapper.selectById(id, userId);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_HISTORY_GET_FAILED", "저장 이력 조회에 실패했습니다.", e);
        }

        if (history == null) {
            throw new BaseException("YOUTUBE_HISTORY_NOT_FOUND", "저장 이력을 찾을 수 없습니다.");
        }
        return history;
    }

    /**
     * PRD 4.2: preprocessed_saved_at 이 null이면 전처리를 수행하고, 완료되면 preprocessed_* 컬럼을 업데이트한다.
     */
    public Path preprocessHistoryIfNeeded(Long historyId, Long userId) {
        YoutubeCommentAnalysisHistory history = findHistoryOrThrow(historyId, userId);
        if (history.getPreprocessedSavedAt() != null && history.getPreprocessedFilePath() != null) {
            return Path.of(history.getPreprocessedFilePath());
        }

        Path original = validateUnderAttachments(history.getOriginalFilePath(), "원본 파일 경로가 올바르지 않습니다.");
        Path out = resolvePreprocessedPathFromOriginal(original);

        try {
            Files.createDirectories(out.getParent());
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_PREPROCESS_DIR_CREATE_FAILED", "전처리 파일 저장 폴더 생성에 실패했습니다.", e);
        }

        int written = 0;
        Set<String> seenCleanTexts = new HashSet<>();
        try (BufferedReader r = Files.newBufferedReader(original, StandardCharsets.UTF_8);
             BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                JsonNode node;
                try {
                    node = objectMapper.readTree(line);
                } catch (Exception ignored) {
                    continue; // skip invalid JSON line
                }

                ObjectNode outNode = (node != null && node.isObject())
                    ? ((ObjectNode) node).deepCopy()
                    : objectMapper.createObjectNode();

                String text = node == null || node.get("text") == null || node.get("text").isNull()
                    ? null
                    : node.get("text").asText();

                // Keep original fields as-is, and add clean output as new fields.
                // This makes the preprocessed JSONL self-contained for later analysis.
                outNode.put("textOriginal", text);
                String clean = preprocessText(text);
                outNode.put("textClean", clean);
                outNode.put("languageHint", inferLanguageHint(clean));

                boolean duplicate = clean != null && !clean.isBlank() && !seenCleanTexts.add(clean);
                outNode.put("isDuplicate", duplicate);

                String spamReason = spamReasonOrNull(text, clean);
                outNode.put("isSpamCandidate", spamReason != null);
                if (spamReason != null) {
                    outNode.put("spamReason", spamReason);
                }

                w.write(objectMapper.writeValueAsString(outNode));
                w.newLine();
                written++;
            }
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_PREPROCESS_FAILED", "댓글 전처리 중 오류가 발생했습니다.", e);
        }

        if (written == 0) {
            throw new BaseException("YOUTUBE_PREPROCESS_EMPTY", "전처리 결과가 비어있습니다(원본 파일 확인 필요).");
        }

        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE_ID);
        int updated;
        try {
            updated = youtubeCommentAnalysisHistoryMapper.updatePreprocessed(historyId, userId, out.toString(), now);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_PREPROCESS_DB_UPDATE_FAILED", "전처리 이력을 DB에 반영하지 못했습니다.", e);
        }

        if (updated <= 0) {
            throw new BaseException("YOUTUBE_PREPROCESS_DB_UPDATE_FAILED", "전처리 이력을 DB에 반영하지 못했습니다.");
        }

        return out;
    }

    public List<YoutubeCommentPreviewRow> loadPreprocessedPreview(Long historyId, Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        YoutubeCommentAnalysisHistory history = findHistoryOrThrow(historyId, userId);
        if (history.getPreprocessedFilePath() == null || history.getPreprocessedSavedAt() == null) {
            return List.of();
        }

        Path preprocessed = validateUnderAttachments(history.getPreprocessedFilePath(), "전처리 파일 경로가 올바르지 않습니다.");
        List<YoutubeCommentPreviewRow> rows = new ArrayList<>(Math.min(100, safeLimit));

        try (BufferedReader r = Files.newBufferedReader(preprocessed, StandardCharsets.UTF_8)) {
            String line;
            while (rows.size() < safeLimit && (line = r.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode node;
                try {
                    node = objectMapper.readTree(line);
                } catch (Exception ignored) {
                    continue;
                }

                YoutubeCommentPreviewRow row = new YoutubeCommentPreviewRow();
                row.setAuthorDisplayName(node.path("authorDisplayName").asText(null));
                row.setPublishedAt(node.path("publishedAt").asText(null));
                row.setLikeCount(node.path("likeCount").isNumber() ? node.path("likeCount").asLong() : null);
                String clean = node.path("textClean").asText(null);
                if (clean == null || clean.isBlank()) {
                    clean = node.path("text_clean").asText(null);
                }
                if (clean == null || clean.isBlank()) {
                    clean = node.path("text").asText(null);
                }
                row.setText(clean);
                rows.add(row);
            }
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_PREPROCESS_PREVIEW_FAILED", "전처리 결과 미리보기 조회에 실패했습니다.", e);
        }

        return rows;
    }

    private Path resolvePreprocessedPathFromOriginal(Path originalJsonl) {
        String fileName = originalJsonl.getFileName() == null ? null : originalJsonl.getFileName().toString();
        String base = (fileName == null || fileName.isBlank()) ? "youtubeComment" : fileName;
        if (base.toLowerCase().endsWith(".jsonl")) {
            base = base.substring(0, base.length() - ".jsonl".length());
        }

        String ts = ZonedDateTime.now(DEFAULT_ZONE_ID).format(EXPORT_DIR_FORMATTER);
        return originalJsonl.getParent().resolve(base + "_clean_" + ts + ".jsonl");
    }

    private Path resolveAttachmentsBaseDir() {
        String baseDir = storageProperties.getAttachmentsDir();
        if (baseDir == null || baseDir.isBlank()) {
            throw new BaseException("STORAGE_ATTACHMENTS_DIR_MISSING", "app.storage.attachments-dir 설정이 비어있습니다.");
        }
        return Path.of(baseDir).normalize().toAbsolutePath();
    }

    private Path validateUnderAttachments(String filePath, String messageForUser) {
        if (filePath == null || filePath.isBlank()) {
            throw new BaseException("STORAGE_PATH_INVALID", messageForUser);
        }

        Path base = resolveAttachmentsBaseDir();
        Path p = Path.of(filePath).normalize().toAbsolutePath();
        if (!p.startsWith(base)) {
            throw new BaseException("STORAGE_PATH_INVALID", messageForUser);
        }
        if (!Files.exists(p)) {
            throw new BaseException("FILE_NOT_FOUND", "파일을 찾을 수 없습니다: " + p);
        }
        return p;
    }

    private static String preprocessText(String text) {
        if (text == null) {
            return null;
        }

        // 1) Basic normalization
        String t = Normalizer.normalize(text, Normalizer.Form.NFKC);
        t = t.replaceAll("[\\r\\n\\t]+", " ");

        // 2) HTML entity/tag cleanup (minimal, for common cases)
        t = decodeBasicHtmlEntities(t);
        t = HTML_TAG_PATTERN.matcher(t).replaceAll(" ");

        // 3) Mentions/hashtags: tokenize to preserve signal (configurable later)
        t = MENTION_PATTERN.matcher(t).replaceAll("@MENTION");
        t = HASHTAG_PATTERN.matcher(t).replaceAll("#TAG");

        // 4) Noise removal
        t = URL_PATTERN.matcher(t).replaceAll(" ");

        // If emojis exist, replace them with a token before stripping symbols
        t = EMOJI_SURROGATE_PATTERN.matcher(t).replaceAll(" EMOJI ");

        // Keep letters/numbers/whitespace and a small set of punctuation; drop the rest.
        t = t.replaceAll("[^\\p{L}\\p{N}\\s\\.,!\\?\"'\\:;\\(\\)\\-\\[\\]\\{\\}/@#%&\\+_=]+", " ");

        // 5) Repeat compression (meaning-preserving)
        t = REPEAT_LAUGH_PATTERN.matcher(t).replaceAll("$1$1");
        t = REPEAT_CRY_PATTERN.matcher(t).replaceAll("$1$1");
        t = t.replaceAll("!{2,}", "!");
        t = t.replaceAll("\\?{2,}", "?");
        t = t.replaceAll("\\.{3,}", "...");

        // 6) Whitespace compaction
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }

    private static String decodeBasicHtmlEntities(String s) {
        if (s == null || s.isBlank()) {
            return s;
        }
        // Keep this minimal and dependency-free.
        return s
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'");
    }

    private static String inferLanguageHint(String clean) {
        if (clean == null || clean.isBlank()) {
            return "unknown";
        }
        int letters = 0;
        int hangul = 0;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (c >= 0xAC00 && c <= 0xD7A3) { // Hangul syllables
                    hangul++;
                }
            }
        }
        if (letters == 0) {
            return "unknown";
        }
        double ratio = (double) hangul / (double) letters;
        return ratio >= 0.20 ? "ko" : "other";
    }

    private static String spamReasonOrNull(String original, String clean) {
        String src = (clean != null && !clean.isBlank()) ? clean : original;
        if (src == null || src.isBlank()) {
            return null;
        }
        String t = src.toLowerCase();

        // Very light heuristics: mark only, do not drop.
        if (t.contains("오픈채팅") || t.contains("openchat") || t.contains("카톡") || t.contains("카카오")) {
            return "chat_contact";
        }
        if (t.contains("t.me") || t.contains("텔레그램") || t.contains("telegram")) {
            return "telegram";
        }
        if (t.contains("bit.ly") || t.contains("tinyurl") || t.contains("shorturl")) {
            return "short_link";
        }
        if (t.contains("구독") && t.contains("좋아요")) {
            return "engagement_bait";
        }
        return null;
    }

    private Path resolveExportPath() {
        String baseDir = storageProperties.getAttachmentsDir();
        if (baseDir == null || baseDir.isBlank()) {
            throw new BaseException("STORAGE_ATTACHMENTS_DIR_MISSING", "app.storage.attachments-dir 설정이 비어있습니다.");
        }
        Path attachments = Path.of(baseDir).normalize();
        String ts = ZonedDateTime.now(DEFAULT_ZONE_ID).format(EXPORT_DIR_FORMATTER);
        return attachments.resolve("youtubeComment").resolve(ts + ".jsonl");
    }

    private static YoutubeCommentJsonlRow mapToJsonlRow(CommentThreadsResponse.Item it) {
        if (it == null || it.getSnippet() == null || it.getSnippet().getTopLevelComment() == null) {
            return null;
        }
        CommentThreadsResponse.TopLevelComment c = it.getSnippet().getTopLevelComment();
        CommentThreadsResponse.CommentSnippet s = c.getSnippet();
        if (s == null) {
            return null;
        }

        YoutubeCommentJsonlRow row = new YoutubeCommentJsonlRow();
        row.setCommentId(c.getId());
        row.setAuthorDisplayName(s.getAuthorDisplayName());
        row.setText(s.getTextOriginal());
        row.setLikeCount(s.getLikeCount());
        row.setPublishedAt(s.getPublishedAt());
        return row;
    }

    public static class YoutubeCommentJsonlRow {
        private String commentId;
        private String authorDisplayName;
        private String text;
        private Long likeCount;
        private String publishedAt;

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

        public String getPublishedAt() {
            return publishedAt;
        }

        public void setPublishedAt(String publishedAt) {
            this.publishedAt = publishedAt;
        }
    }

    public static class YoutubeCommentPreviewRow {
        private String authorDisplayName;
        private String publishedAt;
        private Long likeCount;
        private String text;

        public String getAuthorDisplayName() {
            return authorDisplayName;
        }

        public void setAuthorDisplayName(String authorDisplayName) {
            this.authorDisplayName = authorDisplayName;
        }

        public String getPublishedAt() {
            return publishedAt;
        }

        public void setPublishedAt(String publishedAt) {
            this.publishedAt = publishedAt;
        }

        public Long getLikeCount() {
            return likeCount;
        }

        public void setLikeCount(Long likeCount) {
            this.likeCount = likeCount;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
