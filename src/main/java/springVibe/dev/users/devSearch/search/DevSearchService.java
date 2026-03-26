package springVibe.dev.users.devSearch.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import springVibe.etc.velog.VelogPostEntity;
import springVibe.etc.velog.VelogPostRepository;
import springVibe.etc.velog.VelogPostSummary;
import springVibe.dev.users.devSearch.config.DevSearchElasticsearchProperties;
import springVibe.system.exception.BaseException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Service
public class DevSearchService {
    private final DevSearchElasticsearchProperties properties;
    private final VelogPostRepository velogPostRepository;
    private final ObjectProvider<VelogPostDocumentRepository> documentRepositoryProvider;
    private final ObjectProvider<ElasticsearchOperations> operationsProvider;

    public DevSearchService(
        DevSearchElasticsearchProperties properties,
        VelogPostRepository velogPostRepository,
        ObjectProvider<VelogPostDocumentRepository> documentRepositoryProvider,
        ObjectProvider<ElasticsearchOperations> operationsProvider
    ) {
        this.properties = properties;
        this.velogPostRepository = velogPostRepository;
        this.documentRepositoryProvider = documentRepositoryProvider;
        this.operationsProvider = operationsProvider;
    }

    public record TopTag(String tag, long count) {}

    public record SearchResult(
        long total,
        Long tookMs,
        List<TopTag> topTags,
        Map<String, Double> scores,
        List<VelogPostSummary> items,
        boolean usedElasticsearch
    ) {}

    /**
     * Reindex all DB posts into Elasticsearch. Intended for local/dev usage.
     */
    public int reindexAll() {
        assertEnabled();
        ensureIndex();

        VelogPostDocumentRepository docRepo = documentRepositoryProvider.getIfAvailable();
        if (docRepo == null) {
            throw new BaseException("DEVSEARCH_DISABLED", "DevSearch Elasticsearch is disabled (repositories not enabled).");
        }

        List<VelogPostEntity> all = velogPostRepository.findAll();
        List<VelogPostDocument> docs = new ArrayList<>(all.size());
        for (VelogPostEntity e : all) {
            docs.add(toDocument(e));
        }
        docRepo.saveAll(docs);
        return docs.size();
    }

    /**
     * Delete all documents in the index (keeps index/mapping).
     */
    public void clearAllDocuments() {
        assertEnabled();
        ensureIndex();

        VelogPostDocumentRepository docRepo = documentRepositoryProvider.getIfAvailable();
        if (docRepo == null) {
            throw new BaseException("DEVSEARCH_DISABLED", "DevSearch Elasticsearch is disabled (repositories not enabled).");
        }

        docRepo.deleteAll();
    }

    /**
     * Search via Elasticsearch (when enabled) and then hydrate from DB using the ES hit order.
     * Falls back to DB LIKE search when Elasticsearch is disabled/unavailable.
     */
    public SearchResult search(String q, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        if (q == null || q.isBlank()) {
            return new SearchResult(0, null, List.of(), Map.of(), List.of(), false);
        }

        if (!properties.isEnabled()) {
            return dbLikeSearch(q, safePage, safeSize);
        }

        try {
            ensureIndex();
            ElasticsearchOperations operations = operationsProvider.getIfAvailable();
            if (operations == null) {
                return dbLikeSearch(q, safePage, safeSize);
            }

            NativeQuery query = NativeQuery.builder()
                .withQuery(qb -> qb.bool(b -> b
                    .should(s -> s.match(m -> m.field("title").query(q)))
                    .should(s -> s.match(m -> m.field("body").query(q)))
                    // tags is keyword array; term query is exact match.
                    .should(s -> s.term(t -> t.field("tags").value(v -> v.stringValue(q))))
                    .minimumShouldMatch("1")
                ))
                .withAggregation("top_tags", Aggregation.of(a -> a
                    .terms(t -> t.field("tags").size(10))
                ))
                .withPageable(PageRequest.of(safePage, safeSize))
                .build();

            long started = System.nanoTime();
            SearchHits<VelogPostDocument> hits = operations.search(query, VelogPostDocument.class);
            long tookMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);

            List<String> ids = new ArrayList<>();
            Map<String, Double> scores = new LinkedHashMap<>();
            for (SearchHit<VelogPostDocument> hit : hits.getSearchHits()) {
                VelogPostDocument c = hit.getContent();
                if (c != null && c.getId() != null) {
                    ids.add(c.getId());
                    scores.put(c.getId(), (double) hit.getScore());
                }
            }

            List<TopTag> topTags = extractTopTags(hits);

            if (ids.isEmpty()) {
                return new SearchResult(hits.getTotalHits(), tookMs, topTags, Map.of(), List.of(), true);
            }

            List<VelogPostSummary> rows = velogPostRepository.findByIdIn(ids);
            Map<String, VelogPostSummary> byId = new LinkedHashMap<>();
            for (VelogPostSummary r : rows) {
                if (r != null && r.getId() != null) {
                    byId.put(r.getId(), r);
                }
            }

            List<VelogPostSummary> ordered = new ArrayList<>(ids.size());
            for (String id : ids) {
                VelogPostSummary r = byId.get(id);
                if (r != null) {
                    ordered.add(r);
                }
            }

            return new SearchResult(hits.getTotalHits(), tookMs, topTags, scores, ordered, true);
        } catch (Exception e) {
            // If ES is temporarily down, keep the UI usable.
            return dbLikeSearch(q, safePage, safeSize);
        }
    }

    public List<String> suggestTitles(String prefix, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }
        if (!properties.isEnabled()) {
            return List.of();
        }

        ensureIndex();
        ElasticsearchOperations operations = operationsProvider.getIfAvailable();
        if (operations == null) {
            return List.of();
        }

        NativeQuery query = NativeQuery.builder()
            .withQuery(qb -> qb.matchPhrasePrefix(m -> m.field("title").query(prefix)))
            .withPageable(PageRequest.of(0, Math.min(safeLimit * 5, 50)))
            .build();

        SearchHits<VelogPostDocument> hits = operations.search(query, VelogPostDocument.class);
        Set<String> out = new LinkedHashSet<>();
        for (SearchHit<VelogPostDocument> hit : hits.getSearchHits()) {
            VelogPostDocument d = hit.getContent();
            if (d == null || d.getTitle() == null) continue;
            String t = d.getTitle().trim();
            if (t.isEmpty()) continue;
            out.add(t);
            if (out.size() >= safeLimit) break;
        }
        return List.copyOf(out);
    }

    private void assertEnabled() {
        if (!properties.isEnabled()) {
            throw new BaseException(
                "DEVSEARCH_DISABLED",
                "DevSearch Elasticsearch is disabled. Enable it via DEV_SEARCH_ENABLED=true (local profile)."
            );
        }
    }

    private void ensureIndex() {
        try {
            ElasticsearchOperations operations = operationsProvider.getIfAvailable();
            if (operations == null) {
                throw new IllegalStateException("ElasticsearchOperations bean is not available");
            }
            IndexOperations indexOps = operations.indexOps(VelogPostDocument.class);
            if (!indexOps.exists()) {
                indexOps.createWithMapping();
            }
        } catch (Exception e) {
            throw new BaseException("DEVSEARCH_ES_UNAVAILABLE", "Elasticsearch is not reachable. Start it via docker compose.", e);
        }
    }

    private SearchResult dbLikeSearch(String q, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var p = velogPostRepository.findByTitleContainingOrShortDescriptionContainingOrderByReleasedAtDesc(q, q, pageable);
        return new SearchResult(p.getTotalElements(), null, List.of(), Map.of(), p.getContent(), false);
    }

    private static VelogPostDocument toDocument(VelogPostEntity e) {
        VelogPostDocument d = new VelogPostDocument();
        d.setId(e.getId());
        d.setTitle(e.getTitle());
        d.setBody(e.getBody());
        d.setTags(e.getTags());
        return d;
    }

    private static List<TopTag> extractTopTags(SearchHits<?> hits) {
        if (hits == null || !hits.hasAggregations()) {
            return List.of();
        }
        if (!(hits.getAggregations() instanceof ElasticsearchAggregations aggs)) {
            return List.of();
        }

        var a = aggs.aggregationsAsMap().get("top_tags");
        if (a == null || a.aggregation() == null) {
            return List.of();
        }

        Aggregate raw = a.aggregation().getAggregate();
        if (raw == null || !raw.isSterms()) {
            return List.of();
        }

        var buckets = raw.sterms().buckets();
        if (buckets == null || buckets.array() == null) {
            return List.of();
        }

        List<TopTag> out = new ArrayList<>();
        for (var b : buckets.array()) {
            if (b == null || b.key() == null) continue;
            out.add(new TopTag(b.key().stringValue(), b.docCount()));
        }
        return out;
    }
}
