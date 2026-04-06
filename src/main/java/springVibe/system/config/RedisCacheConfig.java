package springVibe.system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import springVibe.system.cache.CacheNames;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@EnableCaching
@Configuration
public class RedisCacheConfig {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    // Conservative default for LLM translation cache.
    private static final Duration AMAZON_PRODUCT_LLM_TTL = Duration.ofDays(7);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .prefixCacheNameWith("springVibe::")
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(objectMapper)
            ));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(CacheNames.AMAZON_PRODUCT_LLM_KEYWORDS, base.entryTtl(AMAZON_PRODUCT_LLM_TTL));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(base)
            .withInitialCacheConfigurations(perCache)
            .build();
    }

    /**
     * Cache should be a performance optimization only.
     * If Redis is down/misconfigured, ignore cache errors and continue normal execution.
     */
    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.debug("Cache GET error (ignored): cache={} key={}", cache != null ? cache.getName() : "null", key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.debug("Cache PUT error (ignored): cache={} key={}", cache != null ? cache.getName() : "null", key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.debug("Cache EVICT error (ignored): cache={} key={}", cache != null ? cache.getName() : "null", key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.debug("Cache CLEAR error (ignored): cache={}", cache != null ? cache.getName() : "null", exception);
            }
        };
    }
}

