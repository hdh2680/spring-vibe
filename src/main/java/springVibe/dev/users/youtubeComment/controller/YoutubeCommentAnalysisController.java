package springVibe.dev.users.youtubeComment.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import springVibe.dev.common.domain.UserAccount;
import springVibe.dev.common.service.AuthService;
import springVibe.dev.users.youtubeComment.domain.YoutubeCommentAnalysisHistory;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentSearchForm;
import springVibe.dev.users.youtubeComment.service.YoutubeCommentService;
import springVibe.dev.users.youtubeComment.service.YoutubeCommentService.SentimentItemsPage;
import springVibe.dev.users.youtubeComment.service.YoutubeCommentSentimentLlmReviewService;
import springVibe.system.exception.BaseException;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/users/youtubeComment/analysis")
public class YoutubeCommentAnalysisController {
    private final YoutubeCommentService youtubeCommentService;
    private final AuthService authService;
    private final YoutubeCommentSentimentLlmReviewService llmReviewService;

    public YoutubeCommentAnalysisController(
        YoutubeCommentService youtubeCommentService,
        AuthService authService,
        YoutubeCommentSentimentLlmReviewService llmReviewService
    ) {
        this.youtubeCommentService = youtubeCommentService;
        this.authService = authService;
        this.llmReviewService = llmReviewService;
    }

    @GetMapping
    public String list(
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "field", required = false, defaultValue = "all") String field,
        Model model
    ) {
        String query = q == null ? null : q.trim();
        if (query != null && query.isBlank()) {
            query = null;
        }
        String normalizedField = normalizeSearchField(field);
        model.addAttribute("q", query);
        model.addAttribute("field", normalizedField);

        try {
            Long userId = resolveCurrentUserIdOrNull();
            List<YoutubeCommentAnalysisHistory> histories = youtubeCommentService.listHistories(userId, query, normalizedField);
            model.addAttribute("histories", histories);
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }

        return render(model, "유튜브 댓글 분석", "html/users/youtubeComment/youtubeCommentAnalysis");
    }

    /**
     * 상세 화면의 "분석수행" 버튼: 전처리 -> 분석까지 한 번에 수행한다.
     */
    private static String normalizeSearchField(String field) {
        if (field == null) {
            return "all";
        }
        String f = field.trim().toLowerCase();
        return switch (f) {
            case "all", "id", "title", "url", "remark" -> f;
            default -> "all";
        };
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam("id") Long id, Model model) {
        try {
            Long userId = resolveCurrentUserIdOrNull();
            youtubeCommentService.preprocessHistoryIfNeeded(id, userId);
            youtubeCommentService.analyzeAndPersist(id, userId);
            model.addAttribute("successMessage", "분석이 완료되었습니다.");
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }

        return view(id, model);
    }

    /**
     * 댓글 수집 모달의 "분석 수행" 버튼: 저장(이력 생성) -> 전처리 -> 분석까지 한 번에 수행한다.
     */
    @PostMapping(value = "/runAsync", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<RunAsyncResponse> runAsync(
        @Valid @ModelAttribute("form") YoutubeCommentSearchForm form,
        BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().isEmpty()
                ? "입력값이 올바르지 않습니다."
                : bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(RunAsyncResponse.fail("VALIDATION_ERROR", msg));
        }

        try {
            Long userId = resolveCurrentUserIdOrNull();
            Long historyId = youtubeCommentService.runFullAnalysisByUrl(form.getUrl(), userId, form.getRemark());
            return ResponseEntity.ok(RunAsyncResponse.ok(historyId));
        } catch (BaseException e) {
            return ResponseEntity.badRequest().body(RunAsyncResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(RunAsyncResponse.fail("ERR001", e.getMessage()));
        }
    }

    @GetMapping(value = "/result.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> resultJson(@RequestParam("id") Long id) {
        try {
            Long userId = resolveCurrentUserIdOrNull();
            String json = youtubeCommentService.loadLatestAnalysisResultJson(id, userId);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (BaseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonError(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonError("ERR001", e.getMessage()));
        }
    }

    @GetMapping(value = "/sentiment-items.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SentimentItemsPage> sentimentItems(
        @RequestParam("id") Long historyId,
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "label", required = false) String label,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        Long userId = resolveCurrentUserIdOrNull();
        SentimentItemsPage out = youtubeCommentService.listSentimentItems(historyId, userId, q, label, page, size);
        return ResponseEntity.ok(out);
    }

    @GetMapping(value = "/sentiment-summary.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> sentimentSummary(@RequestParam("id") Long historyId) {
        try {
            Long userId = resolveCurrentUserIdOrNull();
            return ResponseEntity.ok(youtubeCommentService.computeSentimentSummaryFromDb(historyId, userId));
        } catch (BaseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("errorCode", e.getCode(), "errorMessage", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("errorCode", "ERR001", "errorMessage", e.getMessage()));
        }
    }

    @PostMapping(value = "/sentiment-items/llm-review", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> llmReview(
        @RequestParam("id") Long historyId,
        @RequestParam("itemIds") List<Long> itemIds
    ) {
        try {
            Long userId = resolveCurrentUserIdOrNull();
            return ResponseEntity.ok(llmReviewService.review(historyId, userId, itemIds));
        } catch (BaseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("errorCode", e.getCode(), "errorMessage", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("errorCode", "ERR001", "errorMessage", e.getMessage()));
        }
    }

    @GetMapping(value = "/lexicon-suggestions.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> pendingLexiconSuggestions(
        @RequestParam("id") Long historyId,
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        try {
            Long userId = resolveCurrentUserIdOrNull();
            return ResponseEntity.ok(llmReviewService.listPending(historyId, userId, limit));
        } catch (BaseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("errorCode", e.getCode(), "errorMessage", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("errorCode", "ERR001", "errorMessage", e.getMessage()));
        }
    }

    @PostMapping(value = "/lexicon-suggestions/apply", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> applyLexiconSuggestions(
        @RequestParam("id") Long historyId,
        @RequestParam("suggestionIds") List<Long> suggestionIds
    ) {
        try {
            Long userId = resolveCurrentUserIdOrNull();
            return ResponseEntity.ok(llmReviewService.applySuggestions(historyId, userId, suggestionIds));
        } catch (BaseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("errorCode", e.getCode(), "errorMessage", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("errorCode", "ERR001", "errorMessage", e.getMessage()));
        }
    }

    @GetMapping("/result/view")
    public String resultView(@RequestParam("id") Long id) {
        return "redirect:/users/youtubeComment/analysis/view?id=" + id;
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") Long id, Model model) {
        try {
            Long userId = resolveCurrentUserIdOrNull();
            YoutubeCommentAnalysisHistory history = youtubeCommentService.findHistoryOrThrow(id, userId);
            model.addAttribute("history", history);
            model.addAttribute("previewRows", youtubeCommentService.loadPreprocessedPreview(id, userId, 200));
            model.addAttribute("preprocessedTotalCount", youtubeCommentService.countPreprocessedComments(id, userId));
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }

        return render(model, "유튜브 댓글 분석", "html/users/youtubeComment/youtubeCommentAnalysisView");
    }

    private static String jsonError(String code, String message) {
        return "{\"errorCode\":\"" + jsonEscape(code) + "\",\"errorMessage\":\"" + jsonEscape(message) + "\"}";
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n");
    }

    private Long resolveCurrentUserIdOrNull() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
                return null;
            }
            UserAccount account = authService.findByUsername(auth.getName());
            return account == null ? null : account.getId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String render(Model model, String pageTitle, String contentTemplate) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("contentTemplate", contentTemplate);
        return "layout/app";
    }

    public static class RunAsyncResponse {
        private boolean success;
        private String message;
        private String errorCode;
        private Long historyId;
        private String redirectUrl;

        public static RunAsyncResponse ok(Long historyId) {
            RunAsyncResponse r = new RunAsyncResponse();
            r.success = true;
            r.message = "ok";
            r.historyId = historyId;
            r.redirectUrl = historyId == null ? null : ("/users/youtubeComment/analysis/view?id=" + historyId);
            return r;
        }

        public static RunAsyncResponse fail(String errorCode, String message) {
            RunAsyncResponse r = new RunAsyncResponse();
            r.success = false;
            r.errorCode = errorCode;
            r.message = message;
            return r;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public Long getHistoryId() {
            return historyId;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }
    }
}
