package springVibe.dev.users.devSearch.controller;

import springVibe.dev.users.devSearch.config.DevSearchElasticsearchProperties;
import springVibe.dev.users.devSearch.search.DevSearchService;
import springVibe.dev.users.devSearch.service.MarkdownRenderService;
import springVibe.etc.velog.VelogPostEntity;
import springVibe.etc.velog.VelogPostRepository;
import springVibe.system.exception.BaseException;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Dev Search pages (Thymeleaf).
 *
 * - List: DB-first. If q is provided and Elasticsearch is enabled, ES is used to pick ids/order, then DB hydrates.
 * - View: DB detail.
 */
@Controller
@RequestMapping("/users/devSearch")
public class DevSearchPagesController {
    private final VelogPostRepository velogPostRepository;
    private final DevSearchService devSearchService;
    private final DevSearchElasticsearchProperties esProps;
    private final MarkdownRenderService markdownRenderService;

    public DevSearchPagesController(
        VelogPostRepository velogPostRepository,
        DevSearchService devSearchService,
        DevSearchElasticsearchProperties esProps,
        MarkdownRenderService markdownRenderService
    ) {
        this.velogPostRepository = velogPostRepository;
        this.devSearchService = devSearchService;
        this.esProps = esProps;
        this.markdownRenderService = markdownRenderService;
    }

    @GetMapping
    public String root() {
        return "redirect:/users/devSearch/list";
    }

    @GetMapping("/list")
    public String list(
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "page", required = false, defaultValue = "0") int page,
        @RequestParam(value = "size", required = false, defaultValue = "20") int size,
        Model model
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        long total = 0L;
        boolean usedElasticsearch = false;
        Long tookMs = null;
        List<DevSearchService.TopTag> topTags = List.of();
        Map<String, Double> scores = Map.of();

        try {
            if (q == null || q.isBlank()) {
                var pageable = PageRequest.of(safePage, safeSize);
                var p = velogPostRepository.findAllByOrderByReleasedAtDesc(pageable);
                model.addAttribute("items", p.getContent());
                total = p.getTotalElements();
                usedElasticsearch = false;
            } else {
                DevSearchService.SearchResult r = devSearchService.search(q, safePage, safeSize);
                model.addAttribute("items", r.items());
                total = r.total();
                usedElasticsearch = r.usedElasticsearch();
                tookMs = r.tookMs();
                topTags = r.topTags();
                scores = r.scores();
            }
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }

        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("page", safePage);
        model.addAttribute("size", safeSize);
        model.addAttribute("esEnabled", esProps.isEnabled());
        model.addAttribute("total", total);
        model.addAttribute("usedElasticsearch", usedElasticsearch);
        model.addAttribute("tookMs", tookMs);
        model.addAttribute("topTags", topTags);
        model.addAttribute("scores", scores);

        int totalPages = computeTotalPages(total, safeSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPrev", safePage > 0);
        model.addAttribute("hasNext", safePage + 1 < totalPages);
        model.addAttribute("prevPage", Math.max(safePage - 1, 0));
        model.addAttribute("nextPage", safePage + 1);

        return render(model, "개발 블로그 검색", "html/users/devSearch/list");
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") String id, Model model) {
        if (id == null || id.isBlank()) {
            throw new BaseException("POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
        }

        VelogPostEntity post = velogPostRepository.findById(id).orElse(null);
        if (post == null) {
            throw new BaseException("POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
        }

        model.addAttribute("post", post);
        model.addAttribute("canonicalUrl", "https://velog.io/@" + post.getUsername() + "/" + post.getUrlSlug());
        model.addAttribute("bodyHtml", markdownRenderService.renderToSafeHtml(post.getBody()));
        return render(model, "개발 블로그 검색", "html/users/devSearch/view");
    }

    @PostMapping("/reindex")
    public String reindex(Model model) {
        try {
            int count = devSearchService.reindexAll();
            model.addAttribute("successMessage", "ES 색인이 완료되었습니다. count=" + count);
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }

        return list("", 0, 20, model);
    }

    private static String render(Model model, String pageTitle, String contentTemplate) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("contentTemplate", contentTemplate);
        return "layout/app";
    }

    private static int computeTotalPages(long total, int size) {
        if (total <= 0L) {
            return 0;
        }
        long pages = (total + size - 1L) / (long) size;
        return pages > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pages;
    }
}
