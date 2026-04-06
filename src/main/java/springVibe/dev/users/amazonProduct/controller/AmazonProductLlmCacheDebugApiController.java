package springVibe.dev.users.amazonProduct.controller;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springVibe.dev.users.amazonProduct.service.AmazonProductLlmKeywordCacheService;
import springVibe.system.cache.CacheNames;

@RestController
@RequestMapping("/api/amazonProduct")
public class AmazonProductLlmCacheDebugApiController {

    private final AmazonProductLlmKeywordCacheService llmKeywordCacheService;
    private final CacheManager cacheManager;

    public AmazonProductLlmCacheDebugApiController(
        AmazonProductLlmKeywordCacheService llmKeywordCacheService,
        CacheManager cacheManager
    ) {
        this.llmKeywordCacheService = llmKeywordCacheService;
        this.cacheManager = cacheManager;
    }

    /**
     * Debug endpoint to verify:
     * - whether LLM translation returns a value
     * - whether Redis cache is populated for the query
     *
     * Usage: GET /api/amazonProduct/llm-keywords?q=야구모자
     */
    @GetMapping("/llm-keywords")
    public DebugResponse llmKeywords(@RequestParam("q") String q) {
        String query = q == null ? "" : q.trim();
        String key = "v1:" + query;

        Cache cache = cacheManager.getCache(CacheNames.AMAZON_PRODUCT_LLM_KEYWORDS);
        String before = cache == null ? null : cache.get(key, String.class);

        String translated = llmKeywordCacheService.translateHangulQueryToEnglishKeywordsCached(query);

        String after = cache == null ? null : cache.get(key, String.class);

        return new DebugResponse(
            query,
            CacheNames.AMAZON_PRODUCT_LLM_KEYWORDS,
            key,
            before,
            translated,
            after
        );
    }

    public record DebugResponse(
        String q,
        String cacheName,
        String cacheKey,
        String cachedBefore,
        String translated,
        String cachedAfter
    ) {}
}
