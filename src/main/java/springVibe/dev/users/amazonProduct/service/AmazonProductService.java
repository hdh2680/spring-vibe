package springVibe.dev.users.amazonProduct.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import springVibe.dev.users.amazonProduct.config.AmazonProductElasticsearchProperties;
import springVibe.dev.users.amazonProduct.domain.AmazonCategory;
import springVibe.dev.users.amazonProduct.domain.AmazonProduct;
import springVibe.dev.users.amazonProduct.repository.AmazonCategoryRepository;
import springVibe.dev.users.amazonProduct.repository.AmazonProductRepository;
import springVibe.dev.users.amazonProduct.repository.AmazonProductRepository.AmazonProductCard;
import springVibe.dev.users.amazonProduct.search.AmazonProductDocument;
import springVibe.dev.users.chat.service.OllamaChatService;
import springVibe.system.exception.BaseException;

import java.util.*;

@Service
public class AmazonProductService {
    private static final Logger log = LoggerFactory.getLogger(AmazonProductService.class);

    private final AmazonProductRepository productRepository;
    private final AmazonCategoryRepository categoryRepository;
    private final AmazonProductElasticsearchProperties esProps;
    private final ObjectProvider<ElasticsearchOperations> operationsProvider;
    private final ObjectProvider<OllamaChatService> ollamaProvider;

    public AmazonProductService(
        AmazonProductRepository productRepository,
        AmazonCategoryRepository categoryRepository,
        AmazonProductElasticsearchProperties esProps,
        ObjectProvider<ElasticsearchOperations> operationsProvider,
        ObjectProvider<OllamaChatService> ollamaProvider
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.esProps = esProps;
        this.operationsProvider = operationsProvider;
        this.ollamaProvider = ollamaProvider;
    }

    public enum SortKey {
        STARS_DESC,
        REVIEWS_DESC,
        PRICE_ASC,
        PRICE_DESC,
        TITLE_ASC
    }

    public record CategoryOption(Long id, String name) {}

    public record ListResult(
        long total,
        Long tookMs,
        boolean usedElasticsearch,
        boolean esEnabled,
        String query,
        String queryUsed,
        Long selectedCategoryId,
        SortKey sortKey,
        List<CategoryOption> categories,
        List<AmazonProductCard> items
    ) {}

    public ListResult list(
        String q,
        Long categoryId,
        SortKey sortKey,
        int page,
        int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        SortKey safeSort = sortKey == null ? SortKey.STARS_DESC : sortKey;
        boolean esEnabled = esProps.isEnabled();

        String query = q == null ? "" : q.trim();
        if (query.isBlank()) {
            List<CategoryOption> categoryOptions = listCategoryOptions();

            long t1 = System.nanoTime();
            var pageable = PageRequest.of(safePage, safeSize, toDbSort(safeSort));
            Page<AmazonProductCard> p = productRepository.listCards(categoryId, pageable);
            long tListMs = (System.nanoTime() - t1) / 1_000_000L;

            if (tListMs > 500) {
                log.info("AmazonProduct list(blank) slow: list={}ms page={} size={} categoryId={} sort={}",
                    tListMs, safePage, safeSize, categoryId, safeSort);
            }
            return new ListResult(
                p.getTotalElements(),
                null,
                false,
                esEnabled,
                query,
                query,
                categoryId,
                safeSort,
                categoryOptions,
                p.getContent()
            );
        }

        if (!esEnabled) {
            return listFromDb(query, categoryId, safeSort, safePage, safeSize);
        }

        try {
            ElasticsearchOperations operations = operationsProvider.getIfAvailable();
            if (operations == null) {
                return listFromDb(query, categoryId, safeSort, safePage, safeSize);
            }

            ensureIndex(operations);

            String queryUsed = maybeTranslateQueryToEnglish(query);
            List<CategoryOption> categoryOptions = listCategoryOptions();

            int fetchLimit = computeEsFetchLimit(safePage, safeSize);

            long started = System.nanoTime();
            SearchHits<AmazonProductDocument> hits = searchAsinsFromEs(operations, queryUsed, fetchLimit);
            long tookMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);

            List<String> asins = new ArrayList<>();
            for (SearchHit<AmazonProductDocument> hit : hits.getSearchHits()) {
                AmazonProductDocument d = hit.getContent();
                if (d != null && d.getAsin() != null) {
                    asins.add(d.getAsin());
                }
            }

            if (asins.isEmpty()) {
                return new ListResult(0L, tookMs, true, esEnabled, query, queryUsed, categoryId, safeSort, categoryOptions, List.of());
            }

            // ES only stores asin+title. Apply category filter/sorting/paging using DB-hydrated rows.
            List<AmazonProductCard> candidates = hydrateCardsInOrder(asins);
            if (categoryId != null) {
                candidates = filterByCategoryId(candidates, categoryId);
            }
            candidates = sortCards(candidates, safeSort);

            long total = candidates.size();
            List<AmazonProductCard> pageItems = slicePage(candidates, safePage, safeSize);

            return new ListResult(total, tookMs, true, esEnabled, query, queryUsed, categoryId, safeSort, categoryOptions, pageItems);
        } catch (Exception e) {
            // Keep the page usable even when ES is down.
            return listFromDb(query, categoryId, safeSort, safePage, safeSize);
        }
    }

    private ListResult listFromDb(String query, Long categoryId, SortKey sortKey, int page, int size) {
        List<CategoryOption> categoryOptions = listCategoryOptions();

        long t1 = System.nanoTime();
        var pageable = PageRequest.of(page, size, toDbSort(sortKey));
        Page<AmazonProductCard> p = productRepository.searchCards(query, categoryId, pageable);
        long tListMs = (System.nanoTime() - t1) / 1_000_000L;

        if (tListMs > 500) {
            log.info("AmazonProduct list(db) slow: list={}ms page={} size={} categoryId={} sort={} qLen={}",
                tListMs, page, size, categoryId, sortKey, query == null ? 0 : query.length());
        }
        return new ListResult(
            p.getTotalElements(),
            null,
            false,
            esProps.isEnabled(),
            query,
            query,
            categoryId,
            sortKey,
            categoryOptions,
            p.getContent()
        );
    }

    private SearchHits<AmazonProductDocument> searchAsinsFromEs(
        ElasticsearchOperations operations,
        String queryUsed,
        int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 20_000);
        var q = NativeQuery.builder()
            .withQuery(qb -> qb.match(m -> m.field("title").query(queryUsed)))
            .withPageable(PageRequest.of(0, safeLimit))
            .build();
        return operations.search(q, AmazonProductDocument.class);
    }

    private static int computeEsFetchLimit(int page, int size) {
        long need = ((long) page + 1L) * (long) size;
        // Over-fetch so category filtering + DB sorting still has enough candidates.
        long over = need * 20L;
        if (over < 500L) over = 500L;
        if (over > 20_000L) over = 20_000L;
        return (int) over;
    }

    private List<AmazonProductCard> hydrateCardsInOrder(List<String> asins) {
        List<AmazonProductCard> rows = productRepository.findCardsByAsinIn(asins);
        Map<String, AmazonProductCard> byAsin = new HashMap<>();
        for (AmazonProductCard r : rows) {
            if (r != null && r.getAsin() != null) {
                byAsin.put(r.getAsin(), r);
            }
        }
        List<AmazonProductCard> ordered = new ArrayList<>(asins.size());
        for (String asin : asins) {
            AmazonProductCard r = byAsin.get(asin);
            if (r != null) ordered.add(r);
        }
        return ordered;
    }

    private static List<AmazonProductCard> filterByCategoryId(List<AmazonProductCard> in, long categoryId) {
        if (in == null || in.isEmpty()) return List.of();
        List<AmazonProductCard> out = new ArrayList<>(in.size());
        for (AmazonProductCard r : in) {
            if (r != null && r.getCategoryId() != null && r.getCategoryId().longValue() == categoryId) {
                out.add(r);
            }
        }
        return out;
    }

    private static List<AmazonProductCard> sortCards(List<AmazonProductCard> in, SortKey sortKey) {
        if (in == null || in.size() <= 1) return in == null ? List.of() : in;
        List<AmazonProductCard> out = new ArrayList<>(in);
        Comparator<AmazonProductCard> c = switch (sortKey) {
            case REVIEWS_DESC -> Comparator
                .comparingInt((AmazonProductCard r) -> r == null || r.getReviews() == null ? Integer.MIN_VALUE : r.getReviews())
                .reversed()
                .thenComparingDouble(r -> r == null || r.getStars() == null ? Double.NEGATIVE_INFINITY : r.getStars());
            case PRICE_ASC -> Comparator
                .comparingDouble((AmazonProductCard r) -> r == null || r.getPrice() == null ? Double.POSITIVE_INFINITY : r.getPrice())
                .thenComparingDouble(r -> r == null || r.getStars() == null ? Double.NEGATIVE_INFINITY : r.getStars()).reversed();
            case PRICE_DESC -> Comparator
                .comparingDouble((AmazonProductCard r) -> r == null || r.getPrice() == null ? Double.NEGATIVE_INFINITY : r.getPrice())
                .reversed()
                .thenComparingDouble(r -> r == null || r.getStars() == null ? Double.NEGATIVE_INFINITY : r.getStars());
            case TITLE_ASC -> Comparator
                .comparing((AmazonProductCard r) -> r == null ? "" : (r.getTitle() == null ? "" : r.getTitle()), String.CASE_INSENSITIVE_ORDER);
            case STARS_DESC -> Comparator
                .comparingDouble((AmazonProductCard r) -> r == null || r.getStars() == null ? Double.NEGATIVE_INFINITY : r.getStars())
                .reversed()
                .thenComparingInt(r -> r == null || r.getReviews() == null ? Integer.MIN_VALUE : r.getReviews());
        };
        out.sort(c);
        return out;
    }

    private static List<AmazonProductCard> slicePage(List<AmazonProductCard> in, int page, int size) {
        if (in == null || in.isEmpty()) return List.of();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        long startL = (long) safePage * (long) safeSize;
        if (startL >= in.size()) return List.of();
        int start = (int) startL;
        int end = Math.min(start + safeSize, in.size());
        return in.subList(start, end);
    }


    private List<CategoryOption> listCategoryOptions() {
        List<AmazonCategory> rows = categoryRepository.findAll();
        if (rows == null || rows.isEmpty()) return List.of();

        List<CategoryOption> out = new ArrayList<>(rows.size());
        for (AmazonCategory c : rows) {
            if (c == null || c.getId() == null) continue;
            out.add(new CategoryOption(c.getId(), displayCategoryName(c)));
        }
        out.sort(Comparator.comparing(CategoryOption::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private static String displayCategoryName(AmazonCategory c) {
        if (c == null) return "";
        String ko = c.getCategoryNameKo();
        if (ko != null && !ko.isBlank()) return ko;
        String en = c.getCategoryName();
        return en == null ? "" : en;
    }

    private static Sort toDbSort(SortKey sortKey) {
        return switch (sortKey) {
            case REVIEWS_DESC -> Sort.by(Sort.Order.desc("reviews"), Sort.Order.desc("stars"));
            case PRICE_ASC -> Sort.by(Sort.Order.asc("price"), Sort.Order.desc("stars"));
            case PRICE_DESC -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("stars"));
            case TITLE_ASC -> Sort.by(Sort.Order.asc("title"));
            case STARS_DESC -> Sort.by(Sort.Order.desc("stars"), Sort.Order.desc("reviews"));
        };
    }

    private void ensureIndex(ElasticsearchOperations operations) {
        try {
            IndexOperations indexOps = operations.indexOps(AmazonProductDocument.class);
            if (!indexOps.exists()) {
                indexOps.createWithMapping();
            }
        } catch (Exception e) {
            throw new BaseException("AMAZON_ES_UNAVAILABLE", "Elasticsearch is not reachable. Start it via docker compose.", e);
        }
    }

    /**
     * Reindex all DB products into Elasticsearch. Intended for local/dev usage.
     */
    public int reindexAll() {
        assertEsEnabled();

        ElasticsearchOperations operations = operationsProvider.getIfAvailable();
        if (operations == null) {
            throw new BaseException("AMAZON_ES_UNAVAILABLE", "ElasticsearchOperations bean is not available.");
        }

        ensureIndex(operations);

        int page = 0;
        int batchSize = clamp(esProps.getReindex().getBatchSize(), 200, 5000);
        int logEvery = clamp(esProps.getReindex().getLogEvery(), 1000, 200_000);
        int indexed = 0;

        while (true) {
            Slice<AmazonProduct> p = productRepository.findAllByOrderByAsinAsc(PageRequest.of(page, batchSize));
            if (p.isEmpty()) break;

            List<AmazonProductDocument> docs = new ArrayList<>(p.getNumberOfElements());
            for (AmazonProduct e : p.getContent()) {
                AmazonProductDocument d = toDocument(e);
                if (d != null && d.getAsin() != null) {
                    docs.add(d);
                }
            }

            if (!docs.isEmpty()) {
                operations.save(docs);
                indexed += docs.size();
            }

            if ((indexed % logEvery) < docs.size()) {
                log.info("AmazonProduct ES reindex progress: indexed={} batchSize={} page={}", indexed, batchSize, page);
            }

            if (!p.hasNext()) break;
            page++;
        }

        try {
            operations.indexOps(AmazonProductDocument.class).refresh();
        } catch (Exception ignore) {
            // Refresh is best-effort for local/dev usage.
        }

        return indexed;
    }

    /**
     * Delete the entire index (and recreate mapping).
     */
    public void clearIndex() {
        assertEsEnabled();

        ElasticsearchOperations operations = operationsProvider.getIfAvailable();
        if (operations == null) {
            throw new BaseException("AMAZON_ES_UNAVAILABLE", "ElasticsearchOperations bean is not available.");
        }

        try {
            IndexOperations indexOps = operations.indexOps(AmazonProductDocument.class);
            if (indexOps.exists()) {
                indexOps.delete();
            }
            indexOps.createWithMapping();
        } catch (Exception e) {
            throw new BaseException("AMAZON_ES_UNAVAILABLE", "Elasticsearch is not reachable. Start it via docker compose.", e);
        }
    }

    private void assertEsEnabled() {
        if (!esProps.isEnabled()) {
            throw new BaseException(
                "AMAZON_ES_DISABLED",
                "AmazonProduct Elasticsearch is disabled. Enable it via AMAZON_PRODUCT_ES_ENABLED=true (local profile)."
            );
        }
    }

    private static AmazonProductDocument toDocument(AmazonProduct e) {
        if (e == null) return null;
        AmazonProductDocument d = new AmazonProductDocument();
        d.setAsin(e.getAsin());
        d.setTitle(e.getTitle());
        return d;
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private String maybeTranslateQueryToEnglish(String q) {
        if (q == null || q.isBlank()) return "";
        // If query already looks like English-ish, don't translate.
        if (!containsHangul(q)) return q;

        OllamaChatService ollama = ollamaProvider.getIfAvailable();
        if (ollama == null || !ollama.isHealthy()) {
            return q;
        }

        List<OllamaChatService.Message> msgs = List.of(
            OllamaChatService.Message.system(
                "You are a translation engine.\n" +
                "Translate the given Korean search query into concise English keywords.\n" +
                "Rules:\n" +
                "- Output ONLY English words.\n" +
                "- Do not include explanations.\n" +
                "- Keep brand/model names as-is."
            ),
            OllamaChatService.Message.user(q)
        );

        String out = ollama.chat(msgs, new OllamaChatService.Options(0.0, 0.9, 32));
        out = sanitizeSingleLine(out);
        if (out.isBlank()) return q;
        if (containsHangulOrCjk(out)) return q;
        return out;
    }

    private static boolean containsHangul(String s) {
        return s != null && s.matches(".*[\\uAC00-\\uD7A3].*");
    }

    private static boolean containsHangulOrCjk(String s) {
        if (s == null || s.isEmpty()) return false;
        return s.matches(".*[\\uAC00-\\uD7A3\\u4E00-\\u9FFF].*");
    }

    private static String sanitizeSingleLine(String s) {
        if (s == null) return "";
        String x = s.trim();
        if ((x.startsWith("\"") && x.endsWith("\"")) || (x.startsWith("'") && x.endsWith("'"))) {
            x = x.substring(1, x.length() - 1).trim();
        }
        String[] lines = x.split("\\r?\\n");
        for (String ln : lines) {
            String t = ln.trim();
            if (!t.isEmpty()) return t;
        }
        return "";
    }
}
