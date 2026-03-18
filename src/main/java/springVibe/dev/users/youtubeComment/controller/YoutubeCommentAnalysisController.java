package springVibe.dev.users.youtubeComment.controller;

import springVibe.dev.common.domain.UserAccount;
import springVibe.dev.common.service.AuthService;
import springVibe.dev.users.youtubeComment.domain.YoutubeCommentAnalysisHistory;
import springVibe.dev.users.youtubeComment.service.YoutubeCommentService;
import springVibe.system.exception.BaseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
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

    @GetMapping("/view")
    public String view(@RequestParam("id") Long id, Model model) {
        try {
            Long userId = resolveCurrentUserIdOrNull();
            YoutubeCommentAnalysisHistory history = youtubeCommentService.findHistoryOrThrow(id, userId);
            model.addAttribute("history", history);
            model.addAttribute("previewRows", youtubeCommentService.loadPreprocessedPreview(id, userId, 200));
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }

        return render(model, "유튜브 댓글 분석", "html/users/youtubeComment/youtubeCommentHistoryView");
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

