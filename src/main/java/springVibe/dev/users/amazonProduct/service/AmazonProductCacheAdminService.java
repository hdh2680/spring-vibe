package springVibe.dev.users.amazonProduct.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Service;
import springVibe.system.cache.CacheNames;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AmazonProductCacheAdminService {
    private static final String KEY_PREFIX = "springVibe::";
    private static final int SCAN_COUNT_HINT = 2000;
    private static final int MAX_KEYS_COLLECT = 50_000; // safety guard for accidental huge scans

    private final RedisConnectionFactory connectionFactory;
    private final GenericJackson2JsonRedisSerializer valueSerializer;
    private final CacheManager cacheManager;

    public AmazonProductCacheAdminService(
        RedisConnectionFactory connectionFactory,
        ObjectMapper objectMapper,
        CacheManager cacheManager
    ) {
        this.connectionFactory = connectionFactory;
        this.valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        this.cacheManager = cacheManager;
    }

    public ListResponse listLlmKeywordCache(String contains, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);

        String redisKeyPrefix = KEY_PREFIX + CacheNames.AMAZON_PRODUCT_LLM_KEYWORDS + "::";
        String pattern = redisKeyPrefix + "*";

        List<String> all = new ArrayList<>();
        try (RedisConnection conn = connectionFactory.getConnection()) {
            var cursor = conn.keyCommands().scan(ScanOptions.scanOptions().match(pattern).count(SCAN_COUNT_HINT).build());
            while (cursor.hasNext()) {
                byte[] k = cursor.next();
                if (k == null || k.length == 0) continue;
                all.add(new String(k, StandardCharsets.UTF_8));
                if (all.size() >= MAX_KEYS_COLLECT) break;
            }
        }

        if (contains != null && !contains.isBlank()) {
            String needle = contains.trim();
            all.removeIf(k -> k == null || !k.contains(needle));
        }

        all.sort(Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

        int total = all.size();
        int from = Math.min(safePage * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<String> pageKeys = all.subList(from, to);

        List<Entry> items = new ArrayList<>(pageKeys.size());
        try (RedisConnection conn = connectionFactory.getConnection()) {
            for (String redisKey : pageKeys) {
                if (redisKey == null) continue;
                String cacheKey = redisKey.startsWith(redisKeyPrefix) ? redisKey.substring(redisKeyPrefix.length()) : redisKey;

                Long ttlSeconds = null;
                try {
                    Long ttl = conn.keyCommands().ttl(redisKey.getBytes(StandardCharsets.UTF_8));
                    if (ttl != null && ttl >= 0) ttlSeconds = ttl;
                } catch (Exception ignored) {
                    // ignore ttl issues
                }

                String value = null;
                try {
                    byte[] raw = conn.stringCommands().get(redisKey.getBytes(StandardCharsets.UTF_8));
                    Object decoded = valueSerializer.deserialize(raw);
                    if (decoded instanceof String s) {
                        value = s;
                    } else if (decoded != null) {
                        value = decoded.toString();
                    }
                } catch (Exception ignored) {
                    // ignore decode issues
                }

                items.add(new Entry(
                    cacheKey,
                    toQuery(cacheKey),
                    ttlSeconds,
                    ttlHuman(ttlSeconds),
                    value,
                    valuePreview(value)
                ));
            }
        }

        return new ListResponse(safePage, safeSize, total, items);
    }

    public void evictLlmKeywordCacheKey(String cacheKey) {
        Cache cache = cacheManager.getCache(CacheNames.AMAZON_PRODUCT_LLM_KEYWORDS);
        if (cache != null && cacheKey != null && !cacheKey.isBlank()) {
            cache.evict(cacheKey);
        }
    }

    public void clearLlmKeywordCache() {
        Cache cache = cacheManager.getCache(CacheNames.AMAZON_PRODUCT_LLM_KEYWORDS);
        if (cache != null) {
            cache.clear();
        }
    }

    private static String toQuery(String cacheKey) {
        if (cacheKey == null) return "";
        String k = cacheKey;
        if (k.startsWith("v1:") || k.startsWith("v2:")) return k.substring(3);
        return k;
    }

    private static String valuePreview(String value) {
        if (value == null) return "";
        int max = 180;
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private static String ttlHuman(Long ttlSeconds) {
        if (ttlSeconds == null) return "";
        Duration d = Duration.ofSeconds(Math.max(0, ttlSeconds));
        long days = d.toDays();
        if (days > 0) return days + "d";
        long hours = d.toHours();
        if (hours > 0) return hours + "h";
        long minutes = d.toMinutes();
        if (minutes > 0) return minutes + "m";
        return ttlSeconds + "s";
    }

    public record Entry(
        String cacheKey,
        String query,
        Long ttlSeconds,
        String ttlHuman,
        String value,
        String valuePreview
    ) {}

    public record ListResponse(int page, int size, int total, List<Entry> items) {}
}
