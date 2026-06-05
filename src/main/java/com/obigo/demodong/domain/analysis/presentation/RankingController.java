package com.obigo.demodong.domain.analysis.presentation;

import com.obigo.demodong.domain.analysis.application.dto.response.RankingEntry;
import com.obigo.demodong.domain.analysis.application.usecase.RankingUseCase;
import com.obigo.demodong.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 전략별 종목 랭킹 API.
 *
 * <p>GET /api/ranking/{strategy}?limit=10</p>
 * <p>strategy: canslim | momentum | seasonality | minervini | magic | piotroski | reversion | total</p>
 */
@Slf4j
@Tag(name = "랭킹", description = "전략별 상위 종목 랭킹")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ranking")
public class RankingController {

    private final RankingUseCase rankingUseCase;

    @Operation(
            summary = "전략별 상위 종목 랭킹",
            description = "분석 히스토리 기반. strategy: canslim / momentum / seasonality / minervini / magic / piotroski / reversion / total"
    )
    @GetMapping("/{strategy}")
    public ApiResponse<List<RankingEntry>> getRanking(
            @Parameter(description = "전략 키", example = "canslim")
            @PathVariable String strategy,
            @Parameter(description = "상위 N개 (1~50, 기본 10)")
            @RequestParam(defaultValue = "10") int limit) {
        log.info("[API] GET /api/ranking/{}?limit={}", strategy, limit);
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return ApiResponse.ok(
                RankingResponseCode.RANKING_SUCCESS,
                rankingUseCase.getRanking(strategy, safeLimit)
        );
    }
}
