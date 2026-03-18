package springVibe.dev.users.youtubeComment.controller;

import springVibe.dev.common.domain.UserAccount;
import springVibe.dev.common.service.AuthService;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentPage;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentSearchForm;
import springVibe.dev.users.youtubeComment.service.YoutubeCommentService;
import springVibe.system.exception.BaseException;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.file.Path;

@Controller
@RequestMapping("/users/youtubeComment/search")
public class YoutubeCommentSearchController {
    private final YoutubeCommentService youtubeCommentService;
    private final AuthService authService;

    public YoutubeCommentSearchController(YoutubeCommentService youtubeCommentService, AuthService authService) {
        this.youtubeCommentService = youtubeCommentService;
        this.authService = authService;
    }

    @GetMapping
    public String search(Model model) {
        YoutubeCommentSearchForm form = new YoutubeCommentSearchForm();
        form.setLimit(50);
        model.addAttribute("form", form);
        return render(model, "유튜브 댓글분석", "html/users/youtubeComment/youtubeCommentSearch");
    }

    @PostMapping
    public String searchSubmit(
        @Valid @ModelAttribute("form") YoutubeCommentSearchForm form,
        BindingResult bindingResult,
        Model model
    ) {
        if (!bindingResult.hasErrors()) {
            try {
                YoutubeCommentPage page = youtubeCommentService.collectCommentsByUrl(form.getUrl(), form.getPageToken(), form.getLimit());
                model.addAttribute("page", page);
            } catch (BaseException e) {
                model.addAttribute("errorCode", e.getCode());
                model.addAttribute("errorMessage", e.getMessage());
            } catch (Exception e) {
                model.addAttribute("errorCode", "ERR001");
                model.addAttribute("errorMessage", e.getMessage());
            }
        }

        return render(model, "유튜브 댓글분석", "html/users/youtubeComment/youtubeCommentSearch");
    }

    @PostMapping("/export")
    public String exportSubmit(
        @Valid @ModelAttribute("form") YoutubeCommentSearchForm form,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            return render(model, "유튜브 댓글분석", "html/users/youtubeComment/youtubeCommentSearch");
        }

        try {
            Long userId = resolveCurrentUserIdOrNull();
            youtubeCommentService.exportAllCommentsByUrlAsJsonlAndSaveHistory(form.getUrl(), userId);
            model.addAttribute("successMessage", "저장되었습니다.");
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }

        return render(model, "유튜브 댓글분석", "html/users/youtubeComment/youtubeCommentSearch");
    }

    @PostMapping(value = "/exportAsync", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ExportAsyncResponse> exportAsync(
        @Valid @ModelAttribute("form") YoutubeCommentSearchForm form,
        BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().isEmpty()
                ? "입력값이 올바르지 않습니다."
                : bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(ExportAsyncResponse.fail("VALIDATION_ERROR", msg));
        }

        try {
            Long userId = resolveCurrentUserIdOrNull();
            Path saved = youtubeCommentService.exportAllCommentsByUrlAsJsonlAndSaveHistory(form.getUrl(), userId);
            return ResponseEntity.ok(ExportAsyncResponse.ok("저장되었습니다.", saved.toString()));
        } catch (BaseException e) {
            return ResponseEntity.internalServerError().body(ExportAsyncResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ExportAsyncResponse.fail("ERR001", e.getMessage()));
        }
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

    public static class ExportAsyncResponse {
        private boolean success;
        private String message;
        private String errorCode;
        private String filePath;

        public static ExportAsyncResponse ok(String message, String filePath) {
            ExportAsyncResponse r = new ExportAsyncResponse();
            r.success = true;
            r.message = message;
            r.filePath = filePath;
            return r;
        }

        public static ExportAsyncResponse fail(String errorCode, String message) {
            ExportAsyncResponse r = new ExportAsyncResponse();
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

        public String getFilePath() {
            return filePath;
        }
    }
}

