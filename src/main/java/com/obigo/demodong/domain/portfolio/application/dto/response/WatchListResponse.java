package com.obigo.demodong.domain.portfolio.application.dto.response;

import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;

public record WatchListResponse(
        Long id,
        String ticker,
        String name,
        MarketType marketType
) {
    public static WatchListResponse from(Stock stock) {
        return new WatchListResponse(
                stock.getId(),
                stock.getTicker(),
                stock.getName(),
                stock.getMarketType()
        );
    }

}
