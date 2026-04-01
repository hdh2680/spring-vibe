package springVibe.dev.users.youtubeComment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import springVibe.dev.users.chat.service.OllamaChatService;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentLexiconSuggestionRow;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentSentimentItemRow;
import springVibe.dev.users.youtubeComment.mapper.YoutubeCommentAnalysisSentimentItemMapper;
import springVibe.dev.users.youtubeComment.mapper.YoutubeCommentLexiconSuggestionMapper;
import springVibe.system.exception.BaseException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class YoutubeCommentSentimentLlmReviewService {
    private static final int MAX_ITEMS_PER_REQUEST = 20;
    private static final int MAX_SUGGESTIONS_PER_ITEM = 10;

    private final OllamaChatService ollamaChatService;
    private final ObjectMapper objectMapper;
    private final YoutubeCommentAnalysisSentimentItemMapper sentimentItemMapper;
    private final YoutubeCommentLexiconSuggestionMapper suggestionMapper;
    private final SentimentCustomLexiconFileService customLexiconFileService;

    public YoutubeCommentSentimentLlmReviewService(
        OllamaChatService ollamaChatService,
        ObjectMapper objectMapper,
        YoutubeCommentAnalysisSentimentItemMapper sentimentItemMapper,
        YoutubeCommentLexiconSuggestionMapper suggestionMapper,
        SentimentCustomLexiconFileService customLexiconFileService
    ) {
        this.ollamaChatService = ollamaChatService;
        this.objectMapper = objectMapper;
        this.sentimentItemMapper = sentimentItemMapper;
        this.suggestionMapper = suggestionMapper;
        this.customLexiconFileService = customLexiconFileService;
    }

    public ReviewResponse review(Long historyId, Long userId, List<Long> ids) {
        if (historyId == null) {
            throw new BaseException("YOUTUBE_HISTORY_ID_REQUIRED", "historyId가 필요합니다.");
        }
        if (ids == null || ids.isEmpty()) {
            throw new BaseException("SENTIMENT_ITEM_IDS_REQUIRED", "대상 댓글을 선택해주세요.");
        }
        if (ids.size() > MAX_ITEMS_PER_REQUEST) {
            throw new BaseException("SENTIMENT_ITEM_IDS_TOO_MANY", "한 번에 최대 " + MAX_ITEMS_PER_REQUEST + "개까지 요청할 수 있습니다.");
        }
        if (!ollamaChatService.isHealthy()) {
            throw new BaseException("OLLAMA_UNAVAILABLE", "Ollama가 실행중인지 확인해주세요.");
        }

        // Dedup while preserving user selection order.
        List<Long> uniqIds = new ArrayList<>(ids.size());
        Set<Long> seen = new HashSet<>();
        for (Long id : ids) {
            if (id == null) continue;
            if (seen.add(id)) uniqIds.add(id);
        }
        if (uniqIds.isEmpty()) {
            throw new BaseException("SENTIMENT_ITEM_IDS_REQUIRED", "대상 댓글을 선택해주세요.");
        }

        List<YoutubeCommentSentimentItemRow> items = sentimentItemMapper.selectByIds(historyId, userId, uniqIds);
        if (items == null || items.isEmpty()) {
            throw new BaseException("SENTIMENT_ITEMS_NOT_FOUND", "대상 댓글을 찾을 수 없습니다.");
        }

        int updated = 0;
        int suggestionInserted = 0;
        for (YoutubeCommentSentimentItemRow it : items) {
            LlmParsed out = callAndParse(it);

            int corrected = (out.label != null && it.getLexLabel() != null && !out.label.equalsIgnoreCase(it.getLexLabel())) ? 1 : 0;
            String finalLabel = out.label == null ? null : out.label;
            String llmLabel = out.label == null ? null : out.label;
            String reason = out.reason;
            sentimentItemMapper.updateLlmResult(historyId, userId, it.getId(), llmLabel, finalLabel, corrected, reason);
            updated++;

            if (!out.suggestions.isEmpty()) {
                suggestionMapper.insertBatch(historyId, userId, it.getId(), out.suggestions);
                suggestionInserted += out.suggestions.size();
            }
        }

        List<YoutubeCommentLexiconSuggestionRow> pending = suggestionMapper.selectPendingByHistoryId(historyId, userId, 200);
        return new ReviewResponse(updated, suggestionInserted, pending);
    }

    public ApplyResponse applySuggestions(Long historyId, Long userId, List<Long> suggestionIds) {
        if (historyId == null) {
            throw new BaseException("YOUTUBE_HISTORY_ID_REQUIRED", "historyId가 필요합니다.");
        }
        if (suggestionIds == null || suggestionIds.isEmpty()) {
            throw new BaseException("SUGGESTION_IDS_REQUIRED", "선택된 제안이 없습니다.");
        }

        List<Long> uniq = new ArrayList<>(suggestionIds.size());
        Set<Long> seen = new HashSet<>();
        for (Long id : suggestionIds) {
            if (id == null) continue;
            if (seen.add(id)) uniq.add(id);
        }
        if (uniq.isEmpty()) {
            throw new BaseException("SUGGESTION_IDS_REQUIRED", "선택된 제안이 없습니다.");
        }

        List<YoutubeCommentLexiconSuggestionRow> rows = suggestionMapper.selectByIds(historyId, userId, uniq);
        if (rows == null || rows.isEmpty()) {
            throw new BaseException("SUGGESTIONS_NOT_FOUND", "제안을 찾을 수 없습니다.");
        }

        int applied = 0;
        int rejected = 0;
        List<Long> appliedIds = new ArrayList<>();
        List<Long> rejectedIds = new ArrayList<>();

        for (YoutubeCommentLexiconSuggestionRow r : rows) {
            if (r.getStatus() == null || !r.getStatus().equalsIgnoreCase("PENDING")) {
                continue;
            }
            String action = (r.getAction() == null ? "" : r.getAction().trim().toUpperCase(Locale.ROOT));
            String term = r.getTerm();
            if ("DELETE".equals(action)) {
                if (customLexiconFileService.containsTerm(term)) {
                    customLexiconFileService.delete(term);
                    applied++;
                    appliedIds.add(r.getId());
                } else {
                    // Do not apply non-existent deletes; keep audit trail as REJECTED.
                    rejected++;
                    rejectedIds.add(r.getId());
                }
            } else {
                // default UPSERT
                customLexiconFileService.upsert(term, r.getScore());
                applied++;
                appliedIds.add(r.getId());
            }
        }

        if (!appliedIds.isEmpty()) {
            suggestionMapper.markAppliedByIds(appliedIds, historyId, userId);
        }
        if (!rejectedIds.isEmpty()) {
            suggestionMapper.markRejectedByIds(rejectedIds, historyId, userId);
        }
        return new ApplyResponse(applied, rejected);
    }

    public List<YoutubeCommentLexiconSuggestionRow> listPending(Long historyId, Long userId, Integer limit) {
        if (historyId == null) {
            throw new BaseException("YOUTUBE_HISTORY_ID_REQUIRED", "historyId가 필요합니다.");
        }
        int l = limit == null ? 200 : limit;
        if (l < 1) l = 1;
        if (l > 500) l = 500;
        return suggestionMapper.selectPendingByHistoryId(historyId, userId, l);
    }

    private LlmParsed callAndParse(YoutubeCommentSentimentItemRow it) {
        String lexLabel = it.getLexLabel() == null ? "" : it.getLexLabel().trim().toUpperCase(Locale.ROOT);
        if (!lexLabel.equals("POSITIVE") && !lexLabel.equals("NEUTRAL") && !lexLabel.equals("NEGATIVE")) {
            lexLabel = "UNKNOWN";
        }
        boolean suggestRequired = it.getMatched() <= 0 || lexLabel.equals("UNKNOWN");

        String sys = ""
            + "You are a sentiment classifier for Korean YouTube comments.\n"
            + "Return ONLY valid JSON. No markdown. No extra text.\n"
            + "Allowed labels: POSITIVE, NEUTRAL, NEGATIVE, UNKNOWN.\n"
            + "If uncertain, use UNKNOWN.\n"
            + "You may propose lexicon suggestions for sentiment_custom.tsv.\n"
            + "When suggestions are required, include 1 to 3 items in the lexicon array.\n"
            + "When suggestions are not required, return an empty lexicon array.\n";

        String user = ""
            + "Comment:\n"
            + (it.getTextClean() == null ? "" : it.getTextClean()) + "\n\n"
            + "Lexicon result:\n"
            + "lex_label=" + safe(it.getLexLabel()) + "\n"
            + "lex_score=" + it.getLexScore() + "\n"
            + "matched=" + it.getMatched() + "\n\n"
            + "Suggestions required: " + (suggestRequired ? "YES" : "NO") + "\n"
            + "If required, suggest lexicon terms that would help lexicon-based sentiment match the chosen label.\n\n"
            + "Output JSON schema:\n"
            + "{\n"
            + "  \"label\": \"POSITIVE|NEUTRAL|NEGATIVE|UNKNOWN\",\n"
            + "  \"reason\": \"short korean reason\",\n"
            + "  \"lexicon\": [\n"
            + "    {\"action\":\"UPSERT|DELETE\",\"term\":\"string\",\"score\":-10,\"reason\":\"why\"}\n"
            + "  ]\n"
            + "}\n";

        String resp = ollamaChatService.chat(List.of(
            OllamaChatService.Message.system(sys),
            OllamaChatService.Message.user(user)
        ), new OllamaChatService.Options(0.0, 0.9, 256));

        return parseJson(resp, suggestRequired);
    }

    private LlmParsed parseJson(String s, boolean suggestRequired) {
        String json = extractJsonObject(s);
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BaseException("OLLAMA_RESPONSE_INVALID", "Ollama 응답 JSON 파싱에 실패했습니다: " + summarize(s), e);
        }

        String label = normalizeLabel(node.path("label").asText(null));
        String reason = trimOrNull(node.path("reason").asText(null));

        List<YoutubeCommentLexiconSuggestionRow> suggestions = new ArrayList<>();
        JsonNode arr = pickSuggestionsNode(node);
        if (arr.isArray()) {
            int n = 0;
            for (JsonNode it : arr) {
                if (n >= MAX_SUGGESTIONS_PER_ITEM) break;
                String action = trimOrNull(it.path("action").asText(null));
                String term = trimOrNull(it.path("term").asText(null));
                int score = it.path("score").isNumber() ? it.path("score").asInt() : 0;
                String why = trimOrNull(it.path("reason").asText(null));
                if (term == null || term.isBlank()) continue;
                if (term.length() > 255) term = term.substring(0, 255);

                String a = action == null ? "UPSERT" : action.trim().toUpperCase(Locale.ROOT);
                if (!a.equals("UPSERT") && !a.equals("DELETE")) {
                    a = "UPSERT";
                }
                if (score < -10) score = -10;
                if (score > 10) score = 10;

                YoutubeCommentLexiconSuggestionRow r = new YoutubeCommentLexiconSuggestionRow();
                r.setAction(a);
                r.setTerm(term);
                r.setScore(score);
                r.setReason(why);
                suggestions.add(r);
                n++;
            }
        }

        if (suggestRequired && suggestions.isEmpty()) {
            // Avoid "0 suggestions" when user explicitly requested suggestions. Keeps UI predictable.
            // LLM might have returned a different key; if nothing parsed, keep an empty list but add hint to reason.
            if (reason == null || reason.isBlank()) {
                reason = "(제안 없음)";
            } else if (!reason.contains("제안")) {
                reason = reason + " (제안 없음)";
            }
        }

        return new LlmParsed(label, reason, suggestions);
    }

    private static JsonNode pickSuggestionsNode(JsonNode node) {
        if (node == null) return null;
        if (node.has("lexicon")) return node.get("lexicon");
        if (node.has("lexiconSuggestions")) return node.get("lexiconSuggestions");
        if (node.has("suggestions")) return node.get("suggestions");
        if (node.has("customLexicon")) return node.get("customLexicon");
        if (node.has("dictionary")) return node.get("dictionary");
        // Some models nest: { lexicon: { items: [...] } }
        JsonNode lx = node.get("lexicon");
        if (lx != null && lx.has("items")) return lx.get("items");
        return node.path("lexicon");
    }

    private static String normalizeLabel(String s) {
        if (s == null) return null;
        String x = s.trim().toUpperCase(Locale.ROOT);
        return switch (x) {
            case "POSITIVE", "NEUTRAL", "NEGATIVE", "UNKNOWN" -> x;
            default -> null;
        };
    }

    private static String extractJsonObject(String s) {
        if (s == null) return "{}";
        String t = s.trim();
        if (t.isEmpty()) return "{}";
        int i = t.indexOf('{');
        int j = t.lastIndexOf('}');
        if (i >= 0 && j > i) {
            return t.substring(i, j + 1);
        }
        return t;
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String summarize(String s) {
        if (s == null) return "";
        String t = s.replace("\r", " ").replace("\n", " ").trim();
        if (t.length() > 240) t = t.substring(0, 240);
        return t;
    }

    private record LlmParsed(String label, String reason, List<YoutubeCommentLexiconSuggestionRow> suggestions) {
    }

    public record ReviewResponse(int updatedCount, int suggestionInsertedCount, List<YoutubeCommentLexiconSuggestionRow> pendingSuggestions) {
    }

    public record ApplyResponse(int appliedCount, int rejectedCount) {
    }
}
