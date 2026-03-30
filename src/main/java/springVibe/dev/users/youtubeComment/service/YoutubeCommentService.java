package springVibe.dev.users.youtubeComment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import springVibe.dev.users.youtubeComment.client.YoutubeDataApiClient;
import springVibe.dev.users.youtubeComment.domain.YoutubeCommentAnalysisHistory;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentItem;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentPage;
import springVibe.dev.users.youtubeComment.dto.youtube.CommentThreadsResponse;
import springVibe.dev.users.youtubeComment.mapper.YoutubeCommentAnalysisHistoryMapper;
import springVibe.dev.users.youtubeComment.mapper.YoutubeCommentAnalysisResultMapper;
import springVibe.dev.users.youtubeComment.sentiment.SentimentAnalyzer;
import springVibe.dev.users.youtubeComment.util.YoutubeUrlParser;
import springVibe.system.storage.StorageProperties;
import springVibe.system.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private static final DateTimeFormatter PUBLISHED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@[\\p{L}\\p{N}_.]{1,50}");
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#[\\p{L}\\p{N}_]{1,50}");
    private static final Pattern EMOJI_SURROGATE_PATTERN = Pattern.compile("[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]");
    private static final Pattern REPEAT_LAUGH_PATTERN = Pattern.compile("([ㅋㅎ])\\1{2,}");
    private static final Pattern REPEAT_CRY_PATTERN = Pattern.compile("([ㅠㅜ])\\1{2,}");
    // After normalization, Hangul jamo can appear as compatibility jamo (ㄱ..ㅎ, ㅏ..ㅣ).
    // Compress "over-repeats" to 2 chars for meaning-preserving output: ㄷㄷㄷ -> ㄷㄷ, ㅋㅋㅋㅋ -> ㅋㅋ, ㅠㅠㅠ -> ㅠㅠ.
    private static final Pattern REPEAT_KOREAN_JAMO_PATTERN = Pattern.compile("([\\u3131-\\u3163])\\1{2,}");
    private static final Pattern KOREAN_JAMO_ONLY_PATTERN = Pattern.compile("^[\\u3131-\\u3163]+$");
    private static final Pattern NUMBER_ONLY_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern EDGE_PUNCT_PATTERN = Pattern.compile("^[\\.,!\\?\"'\\:;\\(\\)\\-\\[\\]\\{\\}/@#%&\\+_=]+|[\\.,!\\?\"'\\:;\\(\\)\\-\\[\\]\\{\\}/@#%&\\+_=]+$");

    private static final Set<String> DEFAULT_STOPWORDS = Set.copyOf(new HashSet<>(List.of(
        "이", "그", "저", "것", "수", "등", "좀", "정말", "진짜", "너무", "완전", "계속", "항상",
        "그리고", "근데", "하지만", "그래서", "그런데", "또", "또는",
        "은", "는", "이", "가", "을", "를", "에", "에서", "에게", "한테", "로", "으로", "와", "과", "도", "만", "까지", "부터", "보다",
        "하다", "되다", "있다", "없다", "이다", "아니다", "같다",
        // common filler/function words (helps network/keyword quality)
        "하고", "때문에", "있는", "이게", "우리", "우리가", "우리도", "우리는", "한다", "라고", "아니라", "무슨", "저런", "모든", "이제", "역시", "많이", "빨리",
        "영상", "유튜브",
        "@mention", "#tag", "emoji"
    )));

    private static final String[] KOR_JOSA_SUFFIXES = new String[]{
        // longer first
        "으로", "에서", "에게", "한테", "까지", "부터",
        "하고", "랑", "과", "와",
        "의", "은", "는", "이", "가", "을", "를", "도", "만", "에", "로"
    };

    private final YoutubeDataApiClient youtubeDataApiClient;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper;
    private final YoutubeCommentAnalysisHistoryMapper youtubeCommentAnalysisHistoryMapper;
    private final YoutubeCommentAnalysisResultMapper youtubeCommentAnalysisResultMapper;
    private final SentimentAnalyzer sentimentAnalyzer;

    public YoutubeCommentService(
        YoutubeDataApiClient youtubeDataApiClient,
        StorageProperties storageProperties,
        ObjectMapper objectMapper,
        YoutubeCommentAnalysisHistoryMapper youtubeCommentAnalysisHistoryMapper,
        YoutubeCommentAnalysisResultMapper youtubeCommentAnalysisResultMapper,
        SentimentAnalyzer sentimentAnalyzer
    ) {
        this.youtubeDataApiClient = youtubeDataApiClient;
        this.storageProperties = storageProperties;
        this.objectMapper = objectMapper;
        this.youtubeCommentAnalysisHistoryMapper = youtubeCommentAnalysisHistoryMapper;
        this.youtubeCommentAnalysisResultMapper = youtubeCommentAnalysisResultMapper;
        this.sentimentAnalyzer = sentimentAnalyzer;
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

        item.setPublishedAtView(formatPublishedAtForView(s.getPublishedAt()));
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

    public Path exportAllCommentsByUrlAsJsonlAndSaveHistory(String inputUrl, Long userId, String remark) {
        Path jsonlPath = exportAllCommentsByUrlAsJsonl(inputUrl);

        YoutubeCommentAnalysisHistory history = new YoutubeCommentAnalysisHistory();
        history.setUserId(userId);
        history.setVideoUrl(inputUrl);
        history.setRemark(remark);
        history.setOriginalFilePath(jsonlPath.toString());
        history.setOriginalSavedAt(LocalDateTime.now(DEFAULT_ZONE_ID));

        // Best-effort: do not fail the export if title lookup fails for any reason.
        try {
            String videoId = YoutubeUrlParser.extractVideoId(inputUrl);
            if (videoId != null && !videoId.isBlank()) {
                history.setVideoTitle(youtubeDataApiClient.getVideoTitle(videoId));
            }
        } catch (Exception ignored) {
            // ignore
        }

        try {
            youtubeCommentAnalysisHistoryMapper.insert(history);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_HISTORY_SAVE_FAILED", "저장 이력을 DB에 남기지 못했습니다.", e);
        }

        return jsonlPath;
    }

    public YoutubeCommentAnalysisHistory exportAllCommentsByUrlAsJsonlAndSaveHistoryRow(String inputUrl, Long userId, String remark) {
        Path jsonlPath = exportAllCommentsByUrlAsJsonl(inputUrl);

        YoutubeCommentAnalysisHistory history = new YoutubeCommentAnalysisHistory();
        history.setUserId(userId);
        history.setVideoUrl(inputUrl);
        history.setRemark(remark);
        history.setOriginalFilePath(jsonlPath.toString());
        history.setOriginalSavedAt(LocalDateTime.now(DEFAULT_ZONE_ID));

        // Best-effort: do not fail the export if title lookup fails for any reason.
        try {
            String videoId = YoutubeUrlParser.extractVideoId(inputUrl);
            if (videoId != null && !videoId.isBlank()) {
                history.setVideoTitle(youtubeDataApiClient.getVideoTitle(videoId));
            }
        } catch (Exception ignored) {
            // ignore
        }

        try {
            youtubeCommentAnalysisHistoryMapper.insert(history);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_HISTORY_SAVE_FAILED", "저장 이력을 DB에 남기지 못했습니다.", e);
        }

        return history;
    }

    public Long runFullAnalysisByUrl(String inputUrl, Long userId, String remark) {
        YoutubeCommentAnalysisHistory history = exportAllCommentsByUrlAsJsonlAndSaveHistoryRow(inputUrl, userId, remark);
        Long historyId = history == null ? null : history.getId();
        if (historyId == null) {
            throw new BaseException("YOUTUBE_HISTORY_SAVE_FAILED", "저장 이력을 생성하지 못했습니다(historyId null).");
        }

        preprocessHistoryIfNeeded(historyId, userId);
        analyzeAndPersist(historyId, userId);
        return historyId;
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
                row.setPublishedAt(formatPublishedAtForView(node.path("publishedAt").asText(null)));
                row.setLikeCount(node.path("likeCount").isNumber() ? node.path("likeCount").asLong() : null);
                String original = node.path("textOriginal").asText(null);
                if (original == null || original.isBlank()) {
                    original = node.path("text_original").asText(null);
                }
                if (original == null || original.isBlank()) {
                    original = node.path("text").asText(null);
                }
                row.setOriginalText(original);

                String clean = node.path("textClean").asText(null);
                if (clean == null || clean.isBlank()) {
                    clean = node.path("text_clean").asText(null);
                }
                if (clean == null || clean.isBlank()) {
                    // Fallback for older JSONL rows that might not have clean fields.
                    clean = original;
                }
                row.setPreprocessedText(clean);
                rows.add(row);
            }
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_PREPROCESS_PREVIEW_FAILED", "전처리 결과 미리보기 조회에 실패했습니다.", e);
        }

        return rows;
    }

    private static String formatPublishedAtForView(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }

        String s = raw.trim();
        try {
            // e.g. 2026-03-16T21:00:10Z
            ZonedDateTime zdt = OffsetDateTime.parse(s).atZoneSameInstant(DEFAULT_ZONE_ID);
            return zdt.format(PUBLISHED_AT_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // Fall through
        } catch (Exception ignored) {
            // Fall through
        }

        // Best-effort normalization for unexpected formats.
        if (s.length() >= 19 && s.charAt(10) == 'T') {
            s = s.replace('T', ' ');
        }
        if (s.endsWith("Z")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public long countPreprocessedComments(Long historyId, Long userId) {
        YoutubeCommentAnalysisHistory history = findHistoryOrThrow(historyId, userId);
        if (history.getPreprocessedFilePath() == null || history.getPreprocessedSavedAt() == null) {
            return 0;
        }

        Path preprocessed = validateUnderAttachments(history.getPreprocessedFilePath(), "전처리 파일 경로가 올바르지 않습니다.");
        try (java.util.stream.Stream<String> stream = Files.lines(preprocessed, StandardCharsets.UTF_8)) {
            return stream.filter(line -> line != null && !line.isBlank()).count();
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_PREPROCESS_COUNT_FAILED", "전처리 댓글 수 조회에 실패했습니다.", e);
        }
    }

    @Transactional
    public void analyzeAndPersist(Long historyId, Long userId) {
        analyzeAndPersist(historyId, userId, 30, 5, true);
    }

    @Transactional
    public void analyzeAndPersist(Long historyId, Long userId, int topN, int clusterK, boolean useBigrams) {
        int safeTopN = Math.max(1, Math.min(topN, 200));
        int safeK = Math.max(1, Math.min(clusterK, 20));

        YoutubeCommentAnalysisHistory history = findHistoryOrThrow(historyId, userId);
        // allow re-run: overwrite analysis result rows for this history_id
        if (false && history.getAnalysisRequestedAt() != null) {
            throw new BaseException("YOUTUBE_ANALYSIS_ALREADY_DONE", "이미 분석이 수행되었습니다(analysis_requested_at가 존재).");
        }
        if (history.getPreprocessedFilePath() == null || history.getPreprocessedSavedAt() == null) {
            throw new BaseException("YOUTUBE_ANALYSIS_PREPROCESS_REQUIRED", "분석을 수행하려면 전처리를 먼저 수행해야 합니다.");
        }

        Path preprocessed = validateUnderAttachments(history.getPreprocessedFilePath(), "전처리 파일 경로가 올바르지 않습니다.");

        // Read comments first (file I/O). Only commit DB state after results are ready.
        List<PreprocessedComment> comments = loadCommentsForAnalysis(preprocessed, 20000);
        if (comments.isEmpty()) {
            throw new BaseException("YOUTUBE_ANALYSIS_EMPTY", "분석 대상 댓글이 비어있습니다(전처리 파일 확인 필요).");
        }

        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE_ID);

        ObjectNode params = objectMapper.createObjectNode();
        params.put("topN", safeTopN);
        params.put("clusterK", safeK);
        params.put("useBigrams", useBigrams);
        params.put("minTokenLen", 2);
        params.put("maxComments", 20000);
        params.put("stopwordsVersion", "default-v1");
        params.put("topicAlgorithm", "seed-keyword-cooccurrence-em-v1");
        params.put("seedSize", 6);
        params.put("minCooccur", 2);
        params.put("emIterations", 3);

        String paramsJson;
        try {
            paramsJson = objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_ANALYSIS_PARAMS_JSON_FAILED", "분석 파라미터 JSON 생성에 실패했습니다.", e);
        }

        String topJson = buildTopKeywordsResultJson(historyId, comments, safeTopN, useBigrams);
        String topicJson = buildTopicGroupsResultJson(historyId, comments, safeTopN, safeK);
        String sentimentJson = buildSentimentResultJson(historyId, comments);
        String networkJson = buildWordNetworkResultJson(historyId, comments);

        int insertedTop;
        int insertedTopic;
        int insertedSentiment;
        int insertedNetwork;
        try {
            youtubeCommentAnalysisResultMapper.deleteTopKeywordsByHistoryId(historyId, history.getUserId());
            youtubeCommentAnalysisResultMapper.deleteTopicGroupsByHistoryId(historyId, history.getUserId());
            youtubeCommentAnalysisResultMapper.deleteSentimentsByHistoryId(historyId, history.getUserId());
            youtubeCommentAnalysisResultMapper.deleteWordNetworksByHistoryId(historyId, history.getUserId());

            insertedTop = youtubeCommentAnalysisResultMapper.insertTopKeywords(historyId, history.getUserId(), now, paramsJson, topJson);
            insertedTopic = youtubeCommentAnalysisResultMapper.insertTopicGroups(historyId, history.getUserId(), now, paramsJson, topicJson);
            insertedSentiment = youtubeCommentAnalysisResultMapper.insertSentiments(historyId, history.getUserId(), now, paramsJson, sentimentJson);
            insertedNetwork = youtubeCommentAnalysisResultMapper.insertWordNetworks(historyId, history.getUserId(), now, paramsJson, networkJson);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_ANALYSIS_DB_INSERT_FAILED", "분석 결과를 DB에 저장하지 못했습니다.", e);
        }

        if (insertedTop <= 0 || insertedTopic <= 0 || insertedSentiment <= 0 || insertedNetwork <= 0) {
            throw new BaseException("YOUTUBE_ANALYSIS_DB_INSERT_FAILED", "분석 결과를 DB에 저장하지 못했습니다.");
        }

        int updated;
        try {
            updated = youtubeCommentAnalysisHistoryMapper.updateAnalysisRequestedAt(historyId, userId, now);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_ANALYSIS_HISTORY_UPDATE_FAILED", "분석 요청 일시를 이력에 반영하지 못했습니다.", e);
        }
        if (updated <= 0) {
            throw new BaseException("YOUTUBE_ANALYSIS_HISTORY_UPDATE_FAILED", "분석 요청 일시를 이력에 반영하지 못했습니다.");
        }
    }
    public String loadLatestAnalysisResultJson(Long historyId, Long userId) {
        YoutubeCommentAnalysisHistory history = findHistoryOrThrow(historyId, userId);
        if (history.getAnalysisRequestedAt() == null) {
            throw new BaseException("YOUTUBE_ANALYSIS_NOT_FOUND", "분석 결과가 없습니다(analysis_requested_at가 null).\n분석을 먼저 수행하세요.");
        }

        String top = youtubeCommentAnalysisResultMapper.selectLatestTopKeywordsResultJson(historyId, userId);
        String topic = youtubeCommentAnalysisResultMapper.selectLatestTopicGroupsResultJson(historyId, userId);
        String sentiment = youtubeCommentAnalysisResultMapper.selectLatestSentimentsResultJson(historyId, userId);
        String network = youtubeCommentAnalysisResultMapper.selectLatestWordNetworksResultJson(historyId, userId);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("historyId", historyId);
        root.put("analysisRequestedAt", history.getAnalysisRequestedAt().toString());

        try {
            if (top != null && !top.isBlank()) {
                root.set("topKeywords", objectMapper.readTree(top));
            } else {
                root.putNull("topKeywords");
            }
            if (topic != null && !topic.isBlank()) {
                root.set("topicGroups", objectMapper.readTree(topic));
            } else {
                root.putNull("topicGroups");
            }
            if (sentiment != null && !sentiment.isBlank()) {
                root.set("sentiments", objectMapper.readTree(sentiment));
            } else {
                root.putNull("sentiments");
            }
            if (network != null && !network.isBlank()) {
                root.set("wordNetwork", objectMapper.readTree(network));
            } else {
                root.putNull("wordNetwork");
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_ANALYSIS_RESULT_JSON_FAILED", "분석 결과 JSON 조합에 실패했습니다.", e);
        }
    }

    private List<PreprocessedComment> loadCommentsForAnalysis(Path preprocessedJsonl, int maxComments) {
        int cap = Math.max(1, Math.min(maxComments, 100_000));
        List<PreprocessedComment> out = new ArrayList<>(Math.min(cap, 2000));
        try (BufferedReader r = Files.newBufferedReader(preprocessedJsonl, StandardCharsets.UTF_8)) {
            String line;
            while (out.size() < cap && (line = r.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode node;
                try {
                    node = objectMapper.readTree(line);
                } catch (Exception ignored) {
                    continue;
                }
                String clean = node.path("textClean").asText(null);
                if (clean == null || clean.isBlank()) {
                    clean = node.path("text_clean").asText(null);
                }
                if (clean == null || clean.isBlank()) {
                    clean = node.path("text").asText(null);
                }
                if (clean == null || clean.isBlank()) {
                    continue;
                }
                String commentId = node.path("commentId").asText(null);
                if (commentId == null || commentId.isBlank()) {
                    commentId = node.path("comment_id").asText(null);
                }

                String publishedAt = node.path("publishedAt").asText(null);
                if (publishedAt == null || publishedAt.isBlank()) {
                    publishedAt = node.path("published_at").asText(null);
                }

                out.add(new PreprocessedComment(commentId, clean, publishedAt));
            }
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_ANALYSIS_READ_FAILED", "전처리 파일 읽기에 실패했습니다.", e);
        }
        return out;
    }

    private String buildTopKeywordsResultJson(Long historyId, List<PreprocessedComment> comments, int topN, boolean useBigrams) {
        Map<String, Long> counts = new HashMap<>(4096);
        for (PreprocessedComment c : comments) {
            List<String> toks = tokenizeForKeywords(c.cleanText);
            for (String t : toks) {
                counts.merge(t, 1L, Long::sum);
            }
            if (useBigrams && toks.size() >= 2) {
                for (int i = 0; i < toks.size() - 1; i++) {
                    String bg = toks.get(i) + " " + toks.get(i + 1);
                    counts.merge(bg, 1L, Long::sum);
                }
            }
        }

        List<Map.Entry<String, Long>> top = counts.entrySet()
            .stream()
            .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .limit(topN)
            .collect(Collectors.toList());

        ObjectNode root = objectMapper.createObjectNode();
        root.put("historyId", historyId);
        root.put("algorithm", "frequency-v1");
        root.put("topN", topN);

        var arr = root.putArray("keywords");
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, Long> e = top.get(i);
            ObjectNode kw = objectMapper.createObjectNode();
            kw.put("rank", i + 1);
            kw.put("term", e.getKey());
            kw.put("count", e.getValue());
            arr.add(kw);
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_ANALYSIS_RESULT_JSON_FAILED", "키워드 분석 결과 JSON 생성에 실패했습니다.", e);
        }
    }

    private String buildWordNetworkResultJson(Long historyId, List<PreprocessedComment> comments) {
        // Word co-occurrence network (comment-level). Nodes are terms, edges connect terms that co-occur in the same comment.
        final int nodeLimit = 60;
        final int edgeLimit = 220;
        final int minDocFreq = 3;
        final int minEdgeWeight = 2;
        final int maxUniqueTokensPerComment = 200;

        // 1) Doc frequency (unique terms per comment)
        Map<String, Integer> docFreq = new HashMap<>(4096);
        List<Set<String>> tokenSets = new ArrayList<>(comments.size());
        for (PreprocessedComment c : comments) {
            List<String> toks = tokenizeForKeywords(c.cleanText);
            if (toks.isEmpty()) {
                tokenSets.add(Set.of());
                continue;
            }
            // Use unique set to avoid order bias (do not just take "first N tokens").
            // Still cap unique terms to keep memory/CPU stable on noisy comments.
            Set<String> set = new HashSet<>(Math.min(maxUniqueTokensPerComment, toks.size()) * 2);
            for (String t : toks) {
                if (set.size() >= maxUniqueTokensPerComment) {
                    break;
                }
                if (t == null || t.isBlank() || DEFAULT_STOPWORDS.contains(t)) {
                    continue;
                }
                set.add(t);
            }
            tokenSets.add(set);
            for (String t : set) {
                docFreq.merge(t, 1, Integer::sum);
            }
        }

        // 2) Pick nodes
        List<String> nodes = docFreq.entrySet()
            .stream()
            .filter(e -> e.getKey() != null && !e.getKey().isBlank() && e.getValue() != null && e.getValue() >= minDocFreq)
            .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .limit(nodeLimit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        Set<String> nodeSet = new HashSet<>(nodes);

        // 3) Build edges among selected nodes
        Map<String, Integer> edgeWeights = new HashMap<>(8192);
        for (Set<String> set : tokenSets) {
            if (set == null || set.size() < 2) {
                continue;
            }
            List<String> terms = set.stream()
                .filter(nodeSet::contains)
                .sorted()
                .collect(Collectors.toList());
            if (terms.size() < 2) {
                continue;
            }
            for (int i = 0; i < terms.size() - 1; i++) {
                String a = terms.get(i);
                for (int j = i + 1; j < terms.size(); j++) {
                    String b = terms.get(j);
                    // stable key: a|b where a < b
                    String key = a + "|" + b;
                    edgeWeights.merge(key, 1, Integer::sum);
                }
            }
        }

        // 4) Filter & sort edges
        List<Map.Entry<String, Integer>> edges = edgeWeights.entrySet()
            .stream()
            .filter(e -> e.getValue() != null && e.getValue() >= minEdgeWeight)
            .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .limit(edgeLimit)
            .collect(Collectors.toList());

        ObjectNode root = objectMapper.createObjectNode();
        root.put("historyId", historyId);
        root.put("algorithm", "cooccurrence-network-v1");
        root.put("nodeLimit", nodeLimit);
        root.put("edgeLimit", edgeLimit);
        root.put("minDocFreq", minDocFreq);
        root.put("minEdgeWeight", minEdgeWeight);
        root.put("maxUniqueTokensPerComment", maxUniqueTokensPerComment);
        root.put("totalComments", comments.size());
        root.put("nodeCount", nodes.size());
        root.put("edgeCount", edges.size());

        var arrNodes = root.putArray("nodes");
        for (String term : nodes) {
            ObjectNode n = objectMapper.createObjectNode();
            n.put("id", term);
            n.put("term", term);
            n.put("docFreq", docFreq.getOrDefault(term, 0));
            arrNodes.add(n);
        }

        var arrEdges = root.putArray("edges");
        for (Map.Entry<String, Integer> e : edges) {
            String key = e.getKey();
            int p = key.indexOf('|');
            if (p <= 0 || p >= key.length() - 1) {
                continue;
            }
            String a = key.substring(0, p);
            String b = key.substring(p + 1);
            ObjectNode row = objectMapper.createObjectNode();
            row.put("source", a);
            row.put("target", b);
            row.put("weight", e.getValue());
            arrEdges.add(row);
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_ANALYSIS_RESULT_JSON_FAILED", "네트워크 분석 결과 JSON 생성에 실패했습니다.", e);
        }
    }

    private String buildSentimentResultJson(Long historyId, List<PreprocessedComment> comments) {
        // Lexicon-based sentiment (max n-gram match) + time series aggregation (3h bucket, %).
        int pos = 0;
        int neu = 0;
        int neg = 0;
        int unk = 0;
        final int maxExamplesPerLabel = 50;

        class Bucket {
            int pos;
            int neu;
            int neg;
            int unk;
            int total() { return pos + neu + neg + unk; }
            int classifiedTotal() { return pos + neu + neg; }
        }

        Map<ZonedDateTime, Bucket> byHour = new HashMap<>();

        // Store a few example comments per label for UI debug/inspection.
        List<ObjectNode> posExamples = new ArrayList<>(maxExamplesPerLabel);
        List<ObjectNode> neuExamples = new ArrayList<>(maxExamplesPerLabel);
        List<ObjectNode> negExamples = new ArrayList<>(maxExamplesPerLabel);
        List<ObjectNode> unkExamples = new ArrayList<>(maxExamplesPerLabel);

        for (PreprocessedComment c : comments) {
            SentimentAnalyzer.Result r = sentimentAnalyzer.analyze(c.cleanText);
            SentimentAnalyzer.Label label = r.label();

            boolean unknown = r.matched() <= 0;
            if (unknown) {
                unk++;
            } else if (label == SentimentAnalyzer.Label.POSITIVE) {
                pos++;
            } else if (label == SentimentAnalyzer.Label.NEGATIVE) {
                neg++;
            } else {
                neu++;
            }

            // Collect examples (max N per label).
            List<ObjectNode> bucket = unknown
                ? unkExamples
                : (label == SentimentAnalyzer.Label.POSITIVE ? posExamples
                : (label == SentimentAnalyzer.Label.NEGATIVE ? negExamples : neuExamples));
            if (bucket.size() < maxExamplesPerLabel) {
                ObjectNode ex = objectMapper.createObjectNode();
                ex.put("commentId", c.commentId);
                ex.put("publishedAt", formatPublishedAtForView(c.publishedAt));
                ex.put("text", c.cleanText);
                ex.put("score", r.score());
                ex.put("matched", r.matched());
                ex.put("label", unknown ? "UNKNOWN" : label.name());
                bucket.add(ex);
            }

            ZonedDateTime hour = parseToHourBucketOrNull(c.publishedAt);
            if (hour != null) {
                Bucket b = byHour.computeIfAbsent(hour, k -> new Bucket());
                if (unknown) {
                    b.unk++;
                } else if (label == SentimentAnalyzer.Label.POSITIVE) b.pos++;
                else if (label == SentimentAnalyzer.Label.NEGATIVE) b.neg++;
                else b.neu++;
            }
        }

        int classifiedTotal = pos + neu + neg;
        int total = classifiedTotal + unk;

        ObjectNode root = objectMapper.createObjectNode();
        root.put("historyId", historyId);
        root.put("algorithm", "lexicon-max-ngram-v1");
        root.put("bucket", "3h");
        root.put("timezone", DEFAULT_ZONE_ID.getId());
        root.put("total", total);
        root.put("classifiedTotal", classifiedTotal);
        root.put("maxExamplesPerLabel", maxExamplesPerLabel);

        ObjectNode overall = root.putObject("overall");
        overall.put("positiveCount", pos);
        overall.put("neutralCount", neu);
        overall.put("negativeCount", neg);
        overall.put("unknownCount", unk);
        overall.put("positivePct", pct(pos, total));
        overall.put("neutralPct", pct(neu, total));
        overall.put("negativePct", pct(neg, total));
        overall.put("unknownPct", pct(unk, total));
        overall.put("classifiedPct", pct(classifiedTotal, total));
        overall.put("positivePctClassified", pct(pos, classifiedTotal));
        overall.put("neutralPctClassified", pct(neu, classifiedTotal));
        overall.put("negativePctClassified", pct(neg, classifiedTotal));

        ObjectNode examples = root.putObject("examples");
        var pArr = examples.putArray("positive");
        for (ObjectNode ex : posExamples) pArr.add(ex);
        var nArr = examples.putArray("neutral");
        for (ObjectNode ex : neuExamples) nArr.add(ex);
        var gArr = examples.putArray("negative");
        for (ObjectNode ex : negExamples) gArr.add(ex);
        var uArr = examples.putArray("unknown");
        for (ObjectNode ex : unkExamples) uArr.add(ex);

        var arr = root.putArray("byHour");
        byHour.keySet()
            .stream()
            .sorted()
            .forEach((hour) -> {
                Bucket b = byHour.get(hour);
                int t = b == null ? 0 : b.total();
                int ct = b == null ? 0 : b.classifiedTotal();
                ObjectNode row = objectMapper.createObjectNode();
                row.put("hour", hour.format(PUBLISHED_AT_FORMATTER));
                row.put("total", t);
                row.put("classifiedTotal", ct);
                row.put("positiveCount", b == null ? 0 : b.pos);
                row.put("neutralCount", b == null ? 0 : b.neu);
                row.put("negativeCount", b == null ? 0 : b.neg);
                row.put("unknownCount", b == null ? 0 : b.unk);
                row.put("positivePct", pct(b == null ? 0 : b.pos, t));
                row.put("neutralPct", pct(b == null ? 0 : b.neu, t));
                row.put("negativePct", pct(b == null ? 0 : b.neg, t));
                row.put("unknownPct", pct(b == null ? 0 : b.unk, t));
                row.put("positivePctClassified", pct(b == null ? 0 : b.pos, ct));
                row.put("neutralPctClassified", pct(b == null ? 0 : b.neu, ct));
                row.put("negativePctClassified", pct(b == null ? 0 : b.neg, ct));
                arr.add(row);
            });

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_ANALYSIS_RESULT_JSON_FAILED", "감정분석 결과 JSON 생성에 실패했습니다.", e);
        }
    }

    private ZonedDateTime parseToHourBucketOrNull(String publishedAt) {
        if (publishedAt == null || publishedAt.isBlank()) {
            return null;
        }
        try {
            // Most JSONL uses ISO-8601 with offset (e.g. 2026-03-19T07:54:21Z).
            OffsetDateTime odt = OffsetDateTime.parse(publishedAt.trim());
            ZonedDateTime hour = odt.atZoneSameInstant(DEFAULT_ZONE_ID).truncatedTo(ChronoUnit.HOURS);
            int h = hour.getHour();
            int bucketStart = (h / 3) * 3;
            return hour.withHour(bucketStart);
        } catch (Exception ignored) {
            // fallback below
        }
        try {
            // Support already-formatted local time (rare).
            LocalDateTime ldt = LocalDateTime.parse(publishedAt.trim(), PUBLISHED_AT_FORMATTER);
            ZonedDateTime hour = ldt.atZone(DEFAULT_ZONE_ID).truncatedTo(ChronoUnit.HOURS);
            int h = hour.getHour();
            int bucketStart = (h / 3) * 3;
            return hour.withHour(bucketStart);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static double pct(int n, int total) {
        if (total <= 0) {
            return 0.0;
        }
        double v = (n * 100.0) / total;
        // Keep it stable for UI by rounding to 1 decimal.
        return Math.round(v * 10.0) / 10.0;
    }

    private String buildTopicGroupsResultJson(Long historyId, List<PreprocessedComment> comments, int seedTopN, int clusterK) {
        // Seed groups: choose representative head terms, then expand each head into a small keyword set
        // using comment-level co-occurrence. Classify comments by overlap with seed keyword sets.
        final int seedSize = 6;
        final int minCooccur = 2;
        final int candidateHeadLimit = Math.max(seedTopN, clusterK * 6);
        final int emIterations = 3;

        final class SeedGroup {
            final String head;
            final List<String> keywords;
            final Set<String> keywordSet;

            SeedGroup(String head, List<String> keywords, Set<String> keywordSet) {
                this.head = head;
                this.keywords = keywords == null ? List.of() : keywords;
                this.keywordSet = keywordSet == null ? Set.of() : keywordSet;
            }
        }

        List<Set<String>> tokenSets = new ArrayList<>(comments.size());
        Map<String, Long> docFreq = new HashMap<>(4096);
        for (PreprocessedComment c : comments) {
            Set<String> set = new HashSet<>(tokenizeForKeywords(c.cleanText));
            tokenSets.add(set);
            for (String t : set) {
                if (t == null || t.isBlank()) {
                    continue;
                }
                docFreq.merge(t, 1L, Long::sum);
            }
        }

        List<String> candidateHeads = docFreq.entrySet()
            .stream()
            .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .limit(candidateHeadLimit)
            .map(Map.Entry::getKey)
            .filter(t -> t != null && !t.isBlank() && !DEFAULT_STOPWORDS.contains(t))
            .collect(Collectors.toList());

        List<SeedGroup> seedGroups = new ArrayList<>(Math.max(1, clusterK));
        for (String head : candidateHeads) {
            int headSupport = 0;
            Map<String, Integer> co = new HashMap<>(256);
            for (Set<String> set : tokenSets) {
                if (set == null || set.isEmpty() || !set.contains(head)) {
                    continue;
                }
                headSupport++;
                for (String t : set) {
                    if (t == null || t.isBlank() || t.equals(head)) {
                        continue;
                    }
                    co.merge(t, 1, Integer::sum);
                }
            }

            // Skip extremely weak heads to avoid noisy topics.
            if (headSupport < 2) {
                continue;
            }

            List<String> keywords = new ArrayList<>(seedSize);
            keywords.add(head);
            co.entrySet()
                .stream()
                .filter(e -> e.getKey() != null && !e.getKey().isBlank() && e.getValue() != null && e.getValue() >= minCooccur)
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                    .thenComparing(Map.Entry::getKey))
                .limit(seedSize * 2L) // extra room, we'll stop at seedSize below
                .forEach(e -> {
                    if (keywords.size() >= seedSize) {
                        return;
                    }
                    String t = e.getKey();
                    if (!keywords.contains(t)) {
                        keywords.add(t);
                    }
                });

            Set<String> kwSet = new HashSet<>(keywords);

            // Enforce topic diversity: skip if too similar to an existing seed group.
            boolean tooSimilar = false;
            for (SeedGroup g : seedGroups) {
                int inter = 0;
                for (String t : kwSet) {
                    if (g.keywordSet.contains(t)) {
                        inter++;
                    }
                }
                int union = kwSet.size() + g.keywordSet.size() - inter;
                double jaccard = union <= 0 ? 0.0 : (inter * 1.0) / union;
                if (jaccard >= 0.60) {
                    tooSimilar = true;
                    break;
                }
            }
            if (tooSimilar) {
                continue;
            }

            seedGroups.add(new SeedGroup(head, keywords, kwSet));
            if (seedGroups.size() >= clusterK) {
                break;
            }
        }

        // Fallback: if co-occurrence seeds were not formed (very small / noisy dataset), fall back to top unigrams.
        if (seedGroups.isEmpty()) {
            List<String> heads = docFreq.entrySet()
                .stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                    .thenComparing(Map.Entry::getKey))
                .limit(Math.max(1, clusterK))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            for (String h : heads) {
                seedGroups.add(new SeedGroup(h, List.of(h), Set.of(h)));
            }
        }

        int k = Math.max(1, Math.min(clusterK, Math.max(1, seedGroups.size())));

        // EM-like refinement: (E) assign comments to best seed group, (M) recompute group keywords from assigned bucket.
        Map<Integer, List<Integer>> clusters = null;
        int[] assignment = new int[comments.size()];
        for (int i = 0; i < assignment.length; i++) {
            assignment[i] = Integer.MIN_VALUE;
        }
        int iterationsRun = 0;
        int movedTotal = 0;

        for (int iter = 0; iter < emIterations; iter++) {
            clusters = new HashMap<>();
            for (int i = 0; i < k; i++) {
                clusters.put(i, new ArrayList<>());
            }
            clusters.put(-1, new ArrayList<>());

            int moved = 0;
            for (int idx = 0; idx < comments.size(); idx++) {
                Set<String> toks = idx < tokenSets.size() ? tokenSets.get(idx) : Set.of();
                int dest = -1;
                int bestScore = 0;
                int best = -1;

                if (toks != null && !toks.isEmpty()) {
                    for (int i = 0; i < k; i++) {
                        SeedGroup g = seedGroups.get(i);
                        int score = 0;
                        if (toks.contains(g.head)) {
                            score += 2;
                        }
                        for (String t : g.keywords) {
                            if (t == null || t.isBlank() || t.equals(g.head)) {
                                continue;
                            }
                            if (toks.contains(t)) {
                                score += 1;
                            }
                        }
                        if (score > bestScore) {
                            bestScore = score;
                            best = i;
                        }
                    }
                    if (bestScore > 0 && best >= 0) {
                        dest = best;
                    }
                }

                if (assignment[idx] != dest) {
                    moved++;
                    assignment[idx] = dest;
                }
                clusters.get(dest).add(idx);
            }

            iterationsRun = iter + 1;
            movedTotal += moved;
            if (moved == 0 || iter == emIterations - 1) {
                break;
            }

            // M-step: update each seed group's keyword set based on current bucket token frequencies.
            List<SeedGroup> refined = new ArrayList<>(k);
            for (int i = 0; i < k; i++) {
                SeedGroup base = seedGroups.get(i);
                List<Integer> bucketIdx = clusters.get(i);
                if (bucketIdx == null || bucketIdx.isEmpty()) {
                    refined.add(base);
                    continue;
                }

                Map<String, Integer> freq = new HashMap<>(512);
                for (int idx : bucketIdx) {
                    Set<String> set = idx < tokenSets.size() ? tokenSets.get(idx) : Set.of();
                    if (set == null || set.isEmpty()) {
                        continue;
                    }
                    for (String t : set) {
                        if (t == null || t.isBlank() || DEFAULT_STOPWORDS.contains(t)) {
                            continue;
                        }
                        freq.merge(t, 1, Integer::sum);
                    }
                }

                List<String> newKeywords = new ArrayList<>(seedSize);
                if (base.head != null && !base.head.isBlank()) {
                    newKeywords.add(base.head);
                }

                freq.entrySet()
                    .stream()
                    .filter(e -> e.getKey() != null && !e.getKey().isBlank() && !e.getKey().equals(base.head))
                    .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(e -> docFreq.getOrDefault(e.getKey(), Long.MAX_VALUE))
                        .thenComparing(Map.Entry::getKey))
                    .limit(seedSize * 4L)
                    .forEach(e -> {
                        if (newKeywords.size() >= seedSize) {
                            return;
                        }
                        newKeywords.add(e.getKey());
                    });

                Set<String> kwSet = new HashSet<>(newKeywords);
                refined.add(new SeedGroup(base.head, newKeywords, kwSet));
            }
            seedGroups = refined;
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("historyId", historyId);
        root.put("algorithm", "seed-keyword-cooccurrence-em-v1");
        root.put("clusterK", k);
        root.put("seedSize", seedSize);
        root.put("minCooccur", minCooccur);
        root.put("candidateHeads", candidateHeadLimit);
        root.put("emIterations", emIterations);
        root.put("iterationsRun", iterationsRun);
        root.put("movedTotal", movedTotal);

        var seedArr = root.putArray("seeds");
        var seedGroupArr = root.putArray("seedKeywordGroups");
        for (int i = 0; i < k; i++) {
            SeedGroup g = seedGroups.get(i);
            seedArr.add(g.head);
            ObjectNode sg = objectMapper.createObjectNode();
            sg.put("seed", g.head);
            var kw = sg.putArray("keywords");
            for (String t : g.keywords) {
                kw.add(t);
            }
            seedGroupArr.add(sg);
        }

        var cArr = root.putArray("clusters");
        for (int i = -1; i < k; i++) {
            List<Integer> bucket = clusters.get(i);
            if (bucket == null || bucket.isEmpty()) {
                continue;
            }
            ObjectNode cNode = objectMapper.createObjectNode();
            cNode.put("clusterId", i);
            if (i >= 0) {
                SeedGroup g = seedGroups.get(i);
                cNode.put("seed", g.head);
                var sk = cNode.putArray("seedKeywords");
                for (String t : g.keywords) {
                    sk.add(t);
                }
            } else {
                cNode.put("seed", "other");
            }
            cNode.put("size", bucket.size());

            Map<String, Long> localCounts = new HashMap<>(2048);
            for (int idx : bucket) {
                PreprocessedComment c = comments.get(idx);
                for (String t : tokenizeForKeywords(c.cleanText)) {
                    localCounts.merge(t, 1L, Long::sum);
                }
            }
            List<Map.Entry<String, Long>> top = localCounts.entrySet()
                .stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                    .thenComparing(Map.Entry::getKey))
                .limit(12)
                .collect(Collectors.toList());

            var kwArr = cNode.putArray("topKeywords");
            for (int r = 0; r < top.size(); r++) {
                Map.Entry<String, Long> e = top.get(r);
                ObjectNode kw = objectMapper.createObjectNode();
                kw.put("rank", r + 1);
                kw.put("term", e.getKey());
                kw.put("count", e.getValue());
                kwArr.add(kw);
            }

            var sampleArr = cNode.putArray("sampleCommentIds");
            int s = 0;
            for (int idx : bucket) {
                PreprocessedComment c = comments.get(idx);
                if (c.commentId != null && !c.commentId.isBlank()) {
                    sampleArr.add(c.commentId);
                    if (++s >= 5) {
                        break;
                    }
                }
            }

            cArr.add(cNode);
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BaseException("YOUTUBE_ANALYSIS_RESULT_JSON_FAILED", "주제별 묶음 결과 JSON 생성에 실패했습니다.", e);
        }
    }

    private List<String> tokenizeForKeywords(String clean) {
        if (clean == null || clean.isBlank()) {
            return List.of();
        }
        // Preprocess output already has URL/HTML stripped; keep tokenization simple and deterministic.
        String[] raw = clean.split("\\s+");
        if (raw.length == 0) {
            return List.of();
        }

        List<String> out = new ArrayList<>(Math.min(16, raw.length));
        for (String r : raw) {
            String t = normalizeToken(r);
            if (t == null) {
                continue;
            }
            String lower = t.toLowerCase(Locale.ROOT);
            if (DEFAULT_STOPWORDS.contains(lower) || DEFAULT_STOPWORDS.contains(t)) {
                continue;
            }
            if (NUMBER_ONLY_PATTERN.matcher(t).matches()) {
                continue;
            }
            if (t.length() < 2) {
                continue;
            }
            if (KOREAN_JAMO_ONLY_PATTERN.matcher(t).matches()) {
                continue;
            }
            out.add(lower);
        }
        return out;
    }

    private String normalizeToken(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        // strip punctuation at the edges
        t = EDGE_PUNCT_PATTERN.matcher(t).replaceAll("");
        t = t.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.equalsIgnoreCase("@MENTION") || t.equalsIgnoreCase("#TAG") || t.equalsIgnoreCase("EMOJI")) {
            return null;
        }

        // Naive particle stripping for Korean (helps merge variants: "미국/미국이/미국은/미국의/미국을").
        // Only strip if token stays at least 2 chars to avoid over-stripping.
        for (String suf : KOR_JOSA_SUFFIXES) {
            if (t.length() <= suf.length() + 1) {
                continue;
            }
            if (t.endsWith(suf)) {
                t = t.substring(0, t.length() - suf.length());
                break;
            }
        }
        return t;
    }

    private static final class PreprocessedComment {
        private final String commentId;
        private final String cleanText;
        private final String publishedAt;

        private PreprocessedComment(String commentId, String cleanText, String publishedAt) {
            this.commentId = commentId;
            this.cleanText = cleanText;
            this.publishedAt = publishedAt;
        }
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
        // NFKC can normalize Hangul compatibility jamo (ㄱ..ㅎ, ㅏ..ㅣ) into Hangul jamo blocks (ᄀ.., ᅡ.., ᆨ..).
        // Convert them back so output stays human-readable and repeat-compression rules work consistently.
        t = mapHangulJamoToCompatibility(t);
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
        t = REPEAT_KOREAN_JAMO_PATTERN.matcher(t).replaceAll("$1$1");
        t = REPEAT_LAUGH_PATTERN.matcher(t).replaceAll("$1$1");
        t = REPEAT_CRY_PATTERN.matcher(t).replaceAll("$1$1");
        t = t.replaceAll("!{2,}", "!");
        t = t.replaceAll("\\?{2,}", "?");
        t = t.replaceAll("\\.{3,}", "...");

        // 6) Whitespace compaction
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }

    private static String mapHangulJamoToCompatibility(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String mapped = mapHangulJamoCharToCompatibilityStringOrNull(c);
            if (mapped == null) {
                if (out != null) {
                    out.append(c);
                }
                continue;
            }
            if (out == null) {
                out = new StringBuilder(s.length() + 8);
                out.append(s, 0, i);
            }
            out.append(mapped);
        }
        return out == null ? s : out.toString();
    }

    private static String mapHangulJamoCharToCompatibilityStringOrNull(char c) {
        // Leading consonants (Choseong) ᄀ..ᄒ
        switch (c) {
            case '\u1100': return "\u3131"; // ᄀ -> ㄱ
            case '\u1101': return "\u3132"; // ᄁ -> ㄲ
            case '\u1102': return "\u3134"; // ᄂ -> ㄴ
            case '\u1103': return "\u3137"; // ᄃ -> ㄷ
            case '\u1104': return "\u3138"; // ᄄ -> ㄸ
            case '\u1105': return "\u3139"; // ᄅ -> ㄹ
            case '\u1106': return "\u3141"; // ᄆ -> ㅁ
            case '\u1107': return "\u3142"; // ᄇ -> ㅂ
            case '\u1108': return "\u3143"; // ᄈ -> ㅃ
            case '\u1109': return "\u3145"; // ᄉ -> ㅅ
            case '\u110A': return "\u3146"; // ᄊ -> ㅆ
            case '\u110B': return "\u3147"; // ᄋ -> ㅇ
            case '\u110C': return "\u3148"; // ᄌ -> ㅈ
            case '\u110D': return "\u3149"; // ᄍ -> ㅉ
            case '\u110E': return "\u314A"; // ᄎ -> ㅊ
            case '\u110F': return "\u314B"; // ᄏ -> ㅋ
            case '\u1110': return "\u314C"; // ᄐ -> ㅌ
            case '\u1111': return "\u314D"; // ᄑ -> ㅍ
            case '\u1112': return "\u314E"; // ᄒ -> ㅎ
        }

        // Vowels (Jungseong) ᅡ..ᅵ
        switch (c) {
            case '\u1161': return "\u314F"; // ᅡ -> ㅏ
            case '\u1162': return "\u3150"; // ᅢ -> ㅐ
            case '\u1163': return "\u3151"; // ᅣ -> ㅑ
            case '\u1164': return "\u3152"; // ᅤ -> ㅒ
            case '\u1165': return "\u3153"; // ᅥ -> ㅓ
            case '\u1166': return "\u3154"; // ᅦ -> ㅔ
            case '\u1167': return "\u3155"; // ᅧ -> ㅕ
            case '\u1168': return "\u3156"; // ᅨ -> ㅖ
            case '\u1169': return "\u3157"; // ᅩ -> ㅗ
            case '\u116A': return "\u3158"; // ᅪ -> ㅘ
            case '\u116B': return "\u3159"; // ᅫ -> ㅙ
            case '\u116C': return "\u315A"; // ᅬ -> ㅚ
            case '\u116D': return "\u315B"; // ᅭ -> ㅛ
            case '\u116E': return "\u315C"; // ᅮ -> ㅜ
            case '\u116F': return "\u315D"; // ᅯ -> ㅝ
            case '\u1170': return "\u315E"; // ᅰ -> ㅞ
            case '\u1171': return "\u315F"; // ᅱ -> ㅟ
            case '\u1172': return "\u3160"; // ᅲ -> ㅠ
            case '\u1173': return "\u3161"; // ᅳ -> ㅡ
            case '\u1174': return "\u3162"; // ᅴ -> ㅢ
            case '\u1175': return "\u3163"; // ᅵ -> ㅣ
        }

        // Trailing consonants (Jongseong) ᆨ..ᇂ
        switch (c) {
            case '\u11A8': return "\u3131"; // ᆨ -> ㄱ
            case '\u11A9': return "\u3132"; // ᆩ -> ㄲ
            case '\u11AA': return "\u3131\u3145"; // ᆪ -> ㄳ
            case '\u11AB': return "\u3134"; // ᆫ -> ㄴ
            case '\u11AC': return "\u3134\u3148"; // ᆬ -> ㄵ
            case '\u11AD': return "\u3134\u314E"; // ᆭ -> ㄶ
            case '\u11AE': return "\u3137"; // ᆮ -> ㄷ
            case '\u11AF': return "\u3139"; // ᆯ -> ㄹ
            case '\u11B0': return "\u3139\u3131"; // ᆰ -> ㄺ
            case '\u11B1': return "\u3139\u3141"; // ᆱ -> ㄻ
            case '\u11B2': return "\u3139\u3142"; // ᆲ -> ㄼ
            case '\u11B3': return "\u3139\u3145"; // ᆳ -> ㄽ
            case '\u11B4': return "\u3139\u314C"; // ᆴ -> ㄾ
            case '\u11B5': return "\u3139\u314D"; // ᆵ -> ㄿ
            case '\u11B6': return "\u3139\u314E"; // ᆶ -> ㅀ
            case '\u11B7': return "\u3141"; // ᆷ -> ㅁ
            case '\u11B8': return "\u3142"; // ᆸ -> ㅂ
            case '\u11B9': return "\u3142\u3145"; // ᆹ -> ㅄ
            case '\u11BA': return "\u3145"; // ᆺ -> ㅅ
            case '\u11BB': return "\u3146"; // ᆻ -> ㅆ
            case '\u11BC': return "\u3147"; // ᆼ -> ㅇ
            case '\u11BD': return "\u3148"; // ᆽ -> ㅈ
            case '\u11BE': return "\u314A"; // ᆾ -> ㅊ
            case '\u11BF': return "\u314B"; // ᆿ -> ㅋ
            case '\u11C0': return "\u314C"; // ᇀ -> ㅌ
            case '\u11C1': return "\u314D"; // ᇁ -> ㅍ
            case '\u11C2': return "\u314E"; // ᇂ -> ㅎ
        }
        return null;
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
        private String originalText;
        private String preprocessedText;

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

        public String getOriginalText() {
            return originalText;
        }

        public void setOriginalText(String originalText) {
            this.originalText = originalText;
        }

        public String getPreprocessedText() {
            return preprocessedText;
        }

        public void setPreprocessedText(String preprocessedText) {
            this.preprocessedText = preprocessedText;
        }

        // Backwards compatibility: older templates/clients referenced "text" for preview.
        public String getText() {
            return preprocessedText;
        }

        public void setText(String text) {
            this.preprocessedText = text;
        }
    }
}
