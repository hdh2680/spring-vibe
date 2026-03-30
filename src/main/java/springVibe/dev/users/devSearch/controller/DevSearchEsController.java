package springVibe.dev.users.devSearch.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springVibe.dev.users.devSearch.search.DevSearchService;

@RestController
@RequestMapping("/users/devSearch/es")
@ConditionalOnProperty(prefix = "dev-search.elasticsearch", name = "enabled", havingValue = "true")
public class DevSearchEsController {
    private final DevSearchService service;

    public DevSearchEsController(DevSearchService service) {
        this.service = service;
    }

    @PostMapping("/reindex-all")
    public String reindex() {
        int count = service.reindexAll();
        return "reindexed:" + count;
    }

    @PostMapping("/clear")
    public String clear() {
        service.clearAllDocuments();
        return "cleared";
    }
}
