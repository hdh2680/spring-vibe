package springVibe.dev.users.youtubeComment.controller;

import springVibe.dev.users.youtubeComment.dto.YoutubeCommentPage;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentSearchForm;
import springVibe.dev.users.youtubeComment.service.YoutubeCommentService;
import springVibe.system.exception.BaseException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users/youtubeComment")
public class YoutubeCommentController {
    private final YoutubeCommentService youtubeCommentService;

    public YoutubeCommentController(YoutubeCommentService youtubeCommentService) {
        this.youtubeCommentService = youtubeCommentService;
    }

    @GetMapping
    public String search(Model model) {
        model.addAttribute("form", new YoutubeCommentSearchForm());
        return render(model, "유튜브 댓글분석", "html/users/youtubeComment/youtubeCommentSearch");
    }

    @PostMapping("/search")
    public String searchSubmit(
        @Valid @ModelAttribute("form") YoutubeCommentSearchForm form,
        BindingResult bindingResult,
        Model model
    ) {
        if (!bindingResult.hasErrors()) {
            try {
                YoutubeCommentPage page = youtubeCommentService.collectCommentsByUrl(form.getUrl(), form.getPageToken());
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

    private static String render(Model model, String pageTitle, String contentTemplate) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("contentTemplate", contentTemplate);
        return "layout/app";
    }
}

