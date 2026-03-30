package springVibe.dev.users.amazonProduct.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import springVibe.dev.users.amazonProduct.service.AmazonProductService;
import springVibe.dev.users.amazonProduct.service.AmazonProductService.ListResult;
import springVibe.system.exception.BaseException;

@Controller
@RequestMapping("/users/amazonProduct")
public class AmazonProductPagesController {
    private final AmazonProductService amazonProductService;

    public AmazonProductPagesController(AmazonProductService amazonProductService) {
        this.amazonProductService = amazonProductService;
    }

    @GetMapping
    public String root() {
        return "redirect:/users/amazonProduct/list";
    }

    @GetMapping("/list")
    public String list(
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "categoryId", required = false) String categoryId,
        @RequestParam(value = "sort", required = false, defaultValue = "STARS_DESC") String sort,
        @RequestParam(value = "page", required = false, defaultValue = "0") int page,
        @RequestParam(value = "size", required = false, defaultValue = "24") int size,
        Model model
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        AmazonProductService.SortKey sortKey = parseSort(sort);
        Long categoryIdLong = parseCategoryId(categoryId);

        long total = 0L;
        boolean usedElasticsearch = false;
        Long tookMs = null;
        boolean esEnabled = false;

        try {
            ListResult r = amazonProductService.list(q, categoryIdLong, sortKey, safePage, safeSize);

            model.addAttribute("q", r.query());
            model.addAttribute("queryUsed", r.queryUsed());
            model.addAttribute("selectedCategoryId", r.selectedCategoryId());
            model.addAttribute("sortKey", r.sortKey().name());

            model.addAttribute("items", r.items());
            model.addAttribute("categories", r.categories());

            total = r.total();
            usedElasticsearch = r.usedElasticsearch();
            tookMs = r.tookMs();
            esEnabled = r.esEnabled();

            int totalPages = computeTotalPages(total, safeSize);
            model.addAttribute("page", safePage);
            model.addAttribute("size", safeSize);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("hasPrev", safePage > 0);
            model.addAttribute("hasNext", safePage + 1 < totalPages);
            model.addAttribute("prevPage", Math.max(safePage - 1, 0));
            model.addAttribute("nextPage", safePage + 1);
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }

        // Defaults to keep Thymeleaf safe even on errors.
        if (model.getAttribute("q") == null) model.addAttribute("q", q == null ? "" : q);
        if (model.getAttribute("queryUsed") == null) model.addAttribute("queryUsed", "");
        if (model.getAttribute("selectedCategoryId") == null) model.addAttribute("selectedCategoryId", categoryIdLong);
        if (model.getAttribute("sortKey") == null) model.addAttribute("sortKey", sortKey.name());
        if (model.getAttribute("items") == null) model.addAttribute("items", java.util.List.of());
        if (model.getAttribute("categories") == null) model.addAttribute("categories", java.util.List.of());

        model.addAttribute("total", total);
        model.addAttribute("tookMs", tookMs);
        model.addAttribute("esEnabled", esEnabled);
        model.addAttribute("usedElasticsearch", usedElasticsearch);

        int totalPages = computeTotalPages(total, safeSize);
        if (model.getAttribute("page") == null) model.addAttribute("page", safePage);
        if (model.getAttribute("size") == null) model.addAttribute("size", safeSize);
        if (model.getAttribute("totalPages") == null) model.addAttribute("totalPages", totalPages);
        if (model.getAttribute("hasPrev") == null) model.addAttribute("hasPrev", safePage > 0);
        if (model.getAttribute("hasNext") == null) model.addAttribute("hasNext", safePage + 1 < totalPages);
        if (model.getAttribute("prevPage") == null) model.addAttribute("prevPage", Math.max(safePage - 1, 0));
        if (model.getAttribute("nextPage") == null) model.addAttribute("nextPage", safePage + 1);

        return render(model, "Amazon Products", "html/users/amazonProduct/list");
    }

    @PostMapping("/reindex")
    public String reindex(Model model) {
        try {
            int count = amazonProductService.reindexAll();
            model.addAttribute("successMessage", "ES 재색인 완료: " + count + "건");
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }
        return list("", null, "STARS_DESC", 0, 24, model);
    }

    @PostMapping("/clear-es")
    public String clearEs(Model model) {
        try {
            amazonProductService.clearIndex();
            model.addAttribute("successMessage", "ES 인덱스 초기화 완료");
        } catch (BaseException e) {
            model.addAttribute("errorCode", e.getCode());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorCode", "ERR001");
            model.addAttribute("errorMessage", e.getMessage());
        }
        return list("", null, "STARS_DESC", 0, 24, model);
    }

    private static Long parseCategoryId(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        try {
            long v = Long.parseLong(t);
            return v > 0 ? v : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static AmazonProductService.SortKey parseSort(String sort) {
        if (sort == null || sort.isBlank()) return AmazonProductService.SortKey.STARS_DESC;
        try {
            return AmazonProductService.SortKey.valueOf(sort.trim().toUpperCase());
        } catch (Exception e) {
            return AmazonProductService.SortKey.STARS_DESC;
        }
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
