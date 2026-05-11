package com.obigo.demodong.domain.portfolio.application.dto.response;

import com.obigo.demodong.domain.portfolio.domain.entity.PortfolioDetail;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PortfolioResponse(
        Long id,
        String ticker,
        String name,
        MarketType marketType,
        int quantity,
        BigDecimal avgPrice,
        BigDecimal currentPrice,
        BigDecimal profitRate
) {
    public static PortfolioResponse from(PortfolioDetail detail, BigDecimal currentPrice) {
        BigDecimal profitRate = null;
        if (currentPrice != null && detail.getAvgPrice().compareTo(BigDecimal.ZERO) != 0) {
            profitRate = currentPrice.subtract(detail.getAvgPrice())
                    .divide(detail.getAvgPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return new PortfolioResponse(
                detail.getId(),
                detail.getStock().getTicker(),
                detail.getStock().getName(),
                detail.getStock().getMarketType(),
                detail.getQuantity(),
                detail.getAvgPrice(),
                currentPrice,
                profitRate
        );
    }
}
