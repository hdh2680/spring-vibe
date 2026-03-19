package springVibe.dev.users.youtubeComment.controller;

import springVibe.dev.common.domain.UserAccount;
import springVibe.dev.common.service.AuthService;
import springVibe.dev.users.youtubeComment.domain.YoutubeCommentAnalysisHistory;
import springVibe.dev.users.youtubeComment.service.YoutubeCommentService;
import springVibe.system.exception.BaseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.util.List;

@Controller
@RequestMapping("/users/youtubeComment/analysis")
public class YoutubeCommentAnalysisController {
    private final YoutubeCommentService youtubeCommentService;
    private final AuthService authService;

    public YoutubeCommentAnalysisController(YoutubeCommentService youtubeCommentService, AuthService authService) {
        this.youtubeCommentService = youtubeCommentService;
        this.authService = authService;
    }

    @GetMapping
    public String list(Model model) {
        try {
            Long userId = resolveCurrentUserIdOrNull();
            List<YoutubeCommentAnalysisHistory> histories = youtubeCommentService.listHistories(userId);
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

    @PostMapping("/preprocess")
    public String preprocess(@RequestParam("id") Long id, Model model) {
        try {
            Long userId = resolveCurrentUserIdOrNull();
            Path saved = youtubeCommentService.preprocessHistoryIfNeeded(id, userId);
            model.addAttribute("successMessage", "전처리가 완료되었습니다.\n" + saved);
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }

        // Re-render list with updated status.
        return list(model);
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam("id") Long id, Model model) {
        try {
            Long userId = resolveCurrentUserIdOrNull();
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

    @GetMapping("/result/view")
    public String resultView(@RequestParam("id") Long id, Model model) {
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
}
