package com.obigo.demodong.domain.analysis.presentation;

import com.obigo.demodong.global.common.response.ApiResponse;
import com.obigo.demodong.global.common.response.GlobalResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 캐시 수동 무효화 어드민 API.
 *
 * <p>엔드포인트:</p>
 * <ul>
 *   <li>DELETE /api/admin/cache/{cacheName}        — 캐시 전체 삭제</li>
 *   <li>DELETE /api/admin/cache/{cacheName}/{key}  — 캐시 단일 키 삭제</li>
 * </ul>
 *
 * <p>유효 캐시명: {@code analysis}, {@code stocks}, {@code marketRegime}</p>
 */
@Tag(name = "캐시 관리", description = "캐시 수동 무효화 (어드민). 유효 캐시: analysis, stocks, marketRegime")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/cache")
public class AdminCacheController {

    private static final Set<String> VALID_CACHES = Set.of("analysis", "stocks", "marketRegime");

    private final CacheManager cacheManager;

    /**
     * 특정 캐시 전체 비우기.
     * 예: DELETE /api/admin/cache/analysis → 모든 종목 분석 캐시 삭제
     */
    @Operation(summary = "캐시 전체 삭제", description = "지정한 캐시의 모든 항목 삭제")
    @DeleteMapping("/{cacheName}")
    public ApiResponse<String> evictCache(@PathVariable String cacheName) {
        if (!VALID_CACHES.contains(cacheName)) {
            return ApiResponse.ok(GlobalResponseCode.SUCCESS,
                    "유효하지 않은 캐시명입니다. 가능한 값: " + VALID_CACHES);
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("[Admin] 캐시 전체 삭제: {}", cacheName);
        }
        return ApiResponse.ok(GlobalResponseCode.SUCCESS, cacheName + " 캐시가 모두 삭제되었습니다.");
    }

    /**
     * 특정 캐시의 단일 키 비우기.
     * 예: DELETE /api/admin/cache/analysis/005930 → 삼성전자 분석 캐시만 삭제
     */
    @Operation(summary = "캐시 단일 키 삭제", description = "예: DELETE /api/admin/cache/analysis/005930 → 삼성전자 분석만 삭제")
    @DeleteMapping("/{cacheName}/{key}")
    public ApiResponse<String> evictCacheKey(@PathVariable String cacheName, @PathVariable String key) {
        if (!VALID_CACHES.contains(cacheName)) {
            return ApiResponse.ok(GlobalResponseCode.SUCCESS,
                    "유효하지 않은 캐시명입니다. 가능한 값: " + VALID_CACHES);
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.info("[Admin] 캐시 키 삭제: {}[{}]", cacheName, key);
        }
        return ApiResponse.ok(GlobalResponseCode.SUCCESS, cacheName + "[" + key + "] 캐시가 삭제되었습니다.");
    }
}
