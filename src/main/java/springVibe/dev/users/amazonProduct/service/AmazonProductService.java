package springVibe.dev.users.amazonProduct.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import springVibe.dev.users.amazonProduct.config.AmazonProductElasticsearchProperties;
import springVibe.dev.users.amazonProduct.domain.AmazonCategory;
import springVibe.dev.users.amazonProduct.repository.AmazonCategoryRepository;
import springVibe.dev.users.amazonProduct.repository.AmazonProductRepository;
import springVibe.dev.users.amazonProduct.repository.AmazonProductRepository.AmazonProductCard;
import springVibe.dev.users.amazonProduct.repository.AmazonProductRepository.CategoryCountRow;
import springVibe.dev.users.amazonProduct.search.AmazonProductDocument;
import springVibe.dev.users.chat.service.OllamaChatService;
import springVibe.system.exception.BaseException;

import java.util.*;

@Service
public class AmazonProductService {
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

    public record CategoryItem(Long id, String name, long count) {}

    public record ListResult(
        long total,
        Long tookMs,
        boolean usedElasticsearch,
        boolean esEnabled,
        String query,
        String queryUsed,
        Long selectedCategoryId,
        SortKey sortKey,
        List<CategoryItem> categories,
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
            List<CategoryItem> cats = buildCategoryItems(topCategoriesFromDb(null), 100);
            var pageable = PageRequest.of(safePage, safeSize, toDbSort(safeSort));
            Page<AmazonProductCard> p = productRepository.searchCards("", categoryId, pageable);
            return new ListResult(
                p.getTotalElements(),
                null,
                false,
                esEnabled,
                query,
                query,
                categoryId,
                safeSort,
                cats,
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

            // Build category list from query only (not category-filtered).
            List<CategoryItem> cats = buildCategoryItems(topCategoriesFromEs(operations, queryUsed), 100);

            long started = System.nanoTime();
            SearchHits<AmazonProductDocument> hits = searchAsinsFromEs(
                operations,
                queryUsed,
                categoryId,
                safeSort,
                safePage,
                safeSize
            );
            long tookMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);

            long total = hits.getTotalHits();
            Long tookMsBoxed = tookMs;

            List<String> asins = new ArrayList<>();
            for (SearchHit<AmazonProductDocument> hit : hits.getSearchHits()) {
                AmazonProductDocument d = hit.getContent();
                if (d != null && d.getAsin() != null) {
                    asins.add(d.getAsin());
                }
            }

            if (asins.isEmpty()) {
                return new ListResult(total, tookMsBoxed, true, esEnabled, query, queryUsed, categoryId, safeSort, cats, List.of());
            }

            List<AmazonProductCard> rows = productRepository.findCardsByAsinIn(asins);
            Map<String, AmazonProductCard> byAsin = new LinkedHashMap<>();
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

            return new ListResult(total, tookMsBoxed, true, esEnabled, query, queryUsed, categoryId, safeSort, cats, ordered);
        } catch (Exception e) {
            // Keep the page usable even when ES is down.
            return listFromDb(query, categoryId, safeSort, safePage, safeSize);
        }
    }

    private ListResult listFromDb(String query, Long categoryId, SortKey sortKey, int page, int size) {
        List<CategoryItem> cats = buildCategoryItems(topCategoriesFromDb(query), 100);
        var pageable = PageRequest.of(page, size, toDbSort(sortKey));
        Page<AmazonProductCard> p = productRepository.searchCards(query, categoryId, pageable);
        return new ListResult(
            p.getTotalElements(),
            null,
            false,
            esProps.isEnabled(),
            query,
            query,
            categoryId,
            sortKey,
            cats,
            p.getContent()
        );
    }

    private List<CategoryCountRow> topCategoriesFromDb(String queryOrNull) {
        var pageable = PageRequest.of(0, 100);
        Page<CategoryCountRow> p = productRepository.countByCategoryForQuery(queryOrNull == null ? "" : queryOrNull, pageable);
        return p.getContent();
    }

    private List<CategoryCountRow> topCategoriesFromEs(ElasticsearchOperations operations, String queryUsed) {
        NativeQuery q = NativeQuery.builder()
            .withQuery(qb -> qb.match(m -> m.field("title").query(queryUsed)))
            .withAggregation("top_categories", Aggregation.of(a -> a
                .terms(t -> t.field("categoryId").size(100))
            ))
            .withPageable(PageRequest.of(0, 1))
            .build();

        SearchHits<AmazonProductDocument> hits = operations.search(q, AmazonProductDocument.class);
        return extractCategoryCounts(hits);
    }

    private SearchHits<AmazonProductDocument> searchAsinsFromEs(
        ElasticsearchOperations operations,
        String queryUsed,
        Long categoryId,
        SortKey sortKey,
        int page,
        int size
    ) {
        var b = NativeQuery.builder()
            .withQuery(qb -> qb.bool(bb -> {
                bb.must(m -> m.match(mm -> mm.field("title").query(queryUsed)));
                if (categoryId != null) {
                    bb.filter(f -> f.term(t -> t.field("categoryId").value(v -> v.longValue(categoryId))));
                }
                return bb;
            }))
            .withPageable(PageRequest.of(page, size));

        // Sorting in ES: if you sort by a field, relevance scoring is effectively ignored.
        // That's acceptable here since UI offers explicit sort options.
        switch (sortKey) {
            case REVIEWS_DESC -> b.withSort(s -> s.field(f -> f.field("reviews").order(SortOrder.Desc)));
            case PRICE_ASC -> b.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Asc)));
            case PRICE_DESC -> b.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Desc)));
            case TITLE_ASC -> b.withSort(s -> s.field(f -> f.field("title").order(SortOrder.Asc)));
            case STARS_DESC -> b.withSort(s -> s.field(f -> f.field("stars").order(SortOrder.Desc)));
        }

        // Secondary tie-breakers.
        b.withSort(s -> s.field(f -> f.field("reviews").order(SortOrder.Desc)));

        return operations.search(b.build(), AmazonProductDocument.class);
    }

    private static List<CategoryCountRow> extractCategoryCounts(SearchHits<?> hits) {
        if (hits == null || !hits.hasAggregations()) {
            return List.of();
        }
        if (!(hits.getAggregations() instanceof ElasticsearchAggregations aggs)) {
            return List.of();
        }

        var a = aggs.aggregationsAsMap().get("top_categories");
        if (a == null || a.aggregation() == null) {
            return List.of();
        }

        Aggregate raw = a.aggregation().getAggregate();
        if (raw == null || !raw.isLterms()) {
            return List.of();
        }

        List<CategoryCountRow> out = new ArrayList<>();
        for (var b : raw.lterms().buckets().array()) {
            Long categoryId = b.key();
            long cnt = b.docCount();
            out.add(new CategoryCountRow() {
                @Override public Long getCategoryId() { return categoryId; }
                @Override public long getCnt() { return cnt; }
            });
        }
        return out;
    }

    private List<CategoryItem> buildCategoryItems(List<CategoryCountRow> rows, int limit) {
        if (rows == null || rows.isEmpty()) return List.of();

        List<Long> ids = new ArrayList<>();
        for (CategoryCountRow r : rows) {
            if (r == null || r.getCategoryId() == null) continue;
            ids.add(r.getCategoryId());
            if (ids.size() >= limit) break;
        }

        Map<Long, AmazonCategory> byId = new HashMap<>();
        for (AmazonCategory c : categoryRepository.findByIdIn(ids)) {
            byId.put(c.getId(), c);
        }

        List<CategoryItem> out = new ArrayList<>();
        for (CategoryCountRow r : rows) {
            if (r == null || r.getCategoryId() == null) continue;
            Long id = r.getCategoryId();
            AmazonCategory c = byId.get(id);
            String name = (c == null) ? ("Category " + id) : displayCategoryName(c);
            out.add(new CategoryItem(id, name, r.getCnt()));
            if (out.size() >= limit) break;
        }
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
