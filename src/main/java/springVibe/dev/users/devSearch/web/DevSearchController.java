package springVibe.dev.users.devSearch.web;

import springVibe.dev.users.devSearch.search.DevSearchService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev-search")
@ConditionalOnProperty(prefix = "dev-search.elasticsearch", name = "enabled", havingValue = "true")
public class DevSearchController {
    private final DevSearchService service;

    public DevSearchController(DevSearchService service) {
        this.service = service;
    }

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }

    @PostMapping("/reindex")
    public String reindex() {
        int count = service.reindexAll();
        return "reindexed:" + count;
    }

    @PostMapping("/clear")
    public String clear() {
        service.clearAllDocuments();
        return "cleared";
    }

    @GetMapping("/search")
    public DevSearchService.SearchResult search(
        @RequestParam String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.search(q, page, size);
    }

    @GetMapping("/suggest")
    public java.util.List<String> suggest(
        @RequestParam("q") String q,
        @RequestParam(value = "limit", required = false, defaultValue = "10") int limit
    ) {
        return service.suggestTitles(q, limit);
    }
}
