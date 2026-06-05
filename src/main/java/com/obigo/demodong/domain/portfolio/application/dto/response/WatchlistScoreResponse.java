package com.obigo.demodong.domain.portfolio.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 관심종목 점수 조회 응답 DTO.
 * 현재가, 등락률, 7개 전략 점수, 종합 점수를 포함한다.
 */
public record WatchlistScoreResponse(
        String ticker,
        String companyName,
        BigDecimal currentPrice,            // nullable — 조회 실패 시
        BigDecimal priceChange,             // 오늘 등락률 (%), nullable
        Map<String, Integer> scores,        // StrategyType.name() → 점수
        Integer totalScore,                 // 7개 전략 평균, nullable
        LocalDateTime analysisUpdatedAt
) {}
