package com.obigo.demodong.domain.portfolio.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 포트폴리오 전체 요약 응답 DTO.
 */
public record PortfolioSummaryResponse(
        BigDecimal totalInvestedAmount,     // 총 투자금액 (avgPrice × quantity 합산)
        BigDecimal totalCurrentAmount,      // 총 평가금액 (currentPrice × quantity 합산)
        BigDecimal totalProfitRate,         // 전체 수익률 (%)
        Integer avgStrategyScore,           // 보유 종목 평균 전략 점수 (nullable)
        List<PortfolioItemDetail> items
) {}
