package com.obigo.demodong.domain.portfolio.domain.service;

import com.obigo.demodong.domain.portfolio.application.dto.response.PortfolioItemDetail;
import com.obigo.demodong.domain.portfolio.application.dto.response.PortfolioSummaryResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 포트폴리오 수익률 집계 도메인 서비스.
 * 순수 계산 로직만 담당 — 외부 의존성 없음.
 */
@Service
public class PortfolioSummaryService {

    public PortfolioSummaryResponse calculate(List<PortfolioItemDetail> items) {
        if (items.isEmpty()) {
            return new PortfolioSummaryResponse(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, items
            );
        }

        BigDecimal totalInvested = items.stream()
                .map(i -> i.avgPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCurrent = items.stream()
                .filter(i -> i.currentPrice() != null)
                .map(i -> i.currentPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfitRate = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) != 0) {
            totalProfitRate = totalCurrent.subtract(totalInvested)
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        Integer avgScore = calcAvgScore(items);

        return new PortfolioSummaryResponse(totalInvested, totalCurrent, totalProfitRate, avgScore, items);
    }

    private Integer calcAvgScore(List<PortfolioItemDetail> items) {
        List<Integer> totals = items.stream()
                .filter(i -> !i.scores().isEmpty())
                .map(i -> (int) i.scores().values().stream().mapToInt(Integer::intValue).average().orElse(0))
                .toList();

        if (totals.isEmpty()) return null;
        return (int) totals.stream().mapToInt(Integer::intValue).average().orElse(0);
    }
}
