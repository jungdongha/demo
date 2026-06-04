package com.obigo.demodong.global.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 기반 인메모리 캐시 설정.
 *
 * <p>캐시 정책:</p>
 * <ul>
 *   <li>analysis      — TTL 1시간 / max 500개  (KIS+DART 호출 캐싱, 장중 최신성 유지)</li>
 *   <li>stocks        — TTL 24시간 / max 1000개 (종목 목록, 변경 빈도 낮음)</li>
 *   <li>marketRegime  — TTL 10분 / max 10개    (KOSPI 지수 기반 시장 국면)</li>
 * </ul>
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.registerCustomCache("analysis",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(500)
                        .build());

        manager.registerCustomCache("stocks",
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(1000)
                        .build());

        manager.registerCustomCache("marketRegime",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(10)
                        .build());

        return manager;
    }
}
