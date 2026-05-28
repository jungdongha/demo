package com.obigo.demodong.domain.analysis.application.dto.response;

import com.obigo.demodong.domain.stock.domain.entity.Stock;

/**
 * 종목 검색 응답 DTO.
 */
public record StockSearchResponse(
        String ticker,
        String name,
        String market,
        String sector
) {
    public static StockSearchResponse from(Stock stock) {
        return new StockSearchResponse(
                stock.getTicker(),
                stock.getName(),
                stock.getMarketType().name(),
                stock.getSector()
        );
    }
}
