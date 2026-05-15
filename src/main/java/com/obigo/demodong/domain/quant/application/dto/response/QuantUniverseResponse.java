package com.obigo.demodong.domain.quant.application.dto.response;

import com.obigo.demodong.domain.quant.domain.entity.QuantUniverse;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;

import java.time.LocalDate;

public record QuantUniverseResponse(
        String ticker,
        String stockName,
        MarketType marketType,
        int marketCapRank,
        String sector,
        LocalDate addedAt
) {
    public static QuantUniverseResponse from(QuantUniverse universe) {
        return new QuantUniverseResponse(
                universe.getTicker(),
                universe.getStockName(),
                universe.getMarketType(),
                universe.getMarketCapRank(),
                universe.getSector(),
                universe.getAddedAt()
        );
    }
}
