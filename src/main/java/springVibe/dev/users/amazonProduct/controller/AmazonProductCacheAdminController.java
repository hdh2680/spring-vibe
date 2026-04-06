package springVibe.dev.users.amazonProduct.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springVibe.dev.users.amazonProduct.service.AmazonProductCacheAdminService;

import java.util.Map;

@RestController
@RequestMapping("/users/amazonProduct/cache")
public class AmazonProductCacheAdminController {

    private final AmazonProductCacheAdminService cacheAdminService;

    public AmazonProductCacheAdminController(AmazonProductCacheAdminService cacheAdminService) {
        this.cacheAdminService = cacheAdminService;
    }

    @GetMapping(value = "/llm-keywords", produces = MediaType.APPLICATION_JSON_VALUE)
    public AmazonProductCacheAdminService.ListResponse listLlmKeywords(
        @RequestParam(value = "contains", required = false) String contains,
        @RequestParam(value = "page", required = false, defaultValue = "0") int page,
        @RequestParam(value = "size", required = false, defaultValue = "25") int size
    ) {
        return cacheAdminService.listLlmKeywordCache(contains, page, size);
    }

    @PostMapping(value = "/llm-keywords/evict", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> evictLlmKeyword(
        @RequestParam("cacheKey") String cacheKey
    ) {
        cacheAdminService.evictLlmKeywordCacheKey(cacheKey);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping(value = "/llm-keywords/clear", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> clearLlmKeywords() {
        cacheAdminService.clearLlmKeywordCache();
        return ResponseEntity.ok(Map.of("success", true));
    }
}

