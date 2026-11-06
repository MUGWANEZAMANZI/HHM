package com.hhm.util;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        ObjectMapper mapper = JsonMapper.builder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        var serializer = RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJacksonJsonRedisSerializer(mapper));
        var keySerializer = RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer());

        // "v3:" prefix — bump this string whenever the Jackson config changes so that
        // entries written with the old format are automatically ignored, not misread.
        // v1: PROPERTY mode — can't embed type ID in a JSON array → START_OBJECT error.
        // v2: WRAPPER_ARRAY — fixed objects, but getAll() used .toList() which returns
        //     ImmutableCollections$ListN (private JDK class). Jackson can't add a type
        //     wrapper it can round-trip for a private class, so the outer List was stored
        //     as a plain array → START_ARRAY error on cache hit.
        // v3: getAll() now uses Collectors.toList() → ArrayList (public, Jackson types it
        //     as ["java.util.ArrayList", [...]]) → round-trips correctly.
        String keyPrefix = "v3:";

        // Base config shared by all caches unless overridden below.
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> keyPrefix + cacheName + "::")
                .disableCachingNullValues()
                .serializeKeysWith(keySerializer)
                .serializeValuesWith(serializer);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        // Single Patient by ID — long TTL, evicted precisely on update/delete.
                        "patient",          defaults.entryTtl(Duration.ofMinutes(60)),
                        // Patient keyword search — shorter TTL, allEntries evicted on any write.
                        "patients-search",  defaults.entryTtl(Duration.ofMinutes(10)),
                        // Full appointment list (AppointmentView, plain POJOs — safe for Redis).
                        // Status filter and search operate on this cached list in memory.
                        // allEntries evicted on any booking, reschedule, cancel, or delete.
                        "appointments",     defaults.entryTtl(Duration.ofMinutes(5))
                ))
                .build();
    }
}
