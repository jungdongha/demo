package com.obigo.demodong.domain.portfolio.application.dto.response;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 포트폴리오 요약 응답 중 종목별 상세 DTO.
 */
public record PortfolioItemDetail(
        String ticker,
        String companyName,
        int quantity,
        BigDecimal avgPrice,
        BigDecimal currentPrice,        // nullable — 조회 실패 시
        BigDecimal profitRate,          // nullable — currentPrice 없으면 null
        Map<String, Integer> scores     // StrategyType.name() → 점수 (분석 캐시 미스 시 빈 Map)
) {}
