package springVibe.dev.users.youtubeComment.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import springVibe.dev.users.devSearch.service.MarkdownRenderService;
import springVibe.system.exception.BaseException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Controller
public class YoutubeCommentHelpController {
    private final MarkdownRenderService markdownRenderService;

    public YoutubeCommentHelpController(MarkdownRenderService markdownRenderService) {
        this.markdownRenderService = markdownRenderService;
    }

    @GetMapping("/users/youtubeComment/help")
    public String help(Model model) {
        try {
            String md = readClasspathUtf8("static/docs/youtubeComment/help.md");
            model.addAttribute("bodyHtml", markdownRenderService.renderToSafeHtml(md));
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }

        model.addAttribute("pageTitle", "유튜브 댓글 분석 도움말");
        model.addAttribute("contentTemplate", "html/users/youtubeComment/youtubeCommentHelp");
        return "layout/app";
    }

    private static String readClasspathUtf8(String path) {
        try {
            ClassPathResource r = new ClassPathResource(path);
            if (!r.exists()) {
                throw new BaseException("HELP_NOT_FOUND", "도움말 파일을 찾을 수 없습니다: " + path);
            }
            try (InputStream in = r.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException("HELP_READ_FAILED", "도움말 파일을 읽지 못했습니다: " + path, e);
        }
    }
}

