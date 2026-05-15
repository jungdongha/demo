package com.obigo.demodong.domain.quant.application.dto.response;

import com.obigo.demodong.domain.quant.domain.entity.QuantSignal;
import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QuantScoreResponse(
        String ticker,
        String stockName,
        MarketType marketType,
        BigDecimal totalScore,
        MarketRegime marketRegime,
        Integer signalRank,
        LocalDate signalDate
) {
    public static QuantScoreResponse from(QuantSignal signal) {
        return new QuantScoreResponse(
                signal.getTicker(),
                signal.getStockName(),
                signal.getMarketType(),
                signal.getTotalScore(),
                signal.getMarketRegime(),
                signal.getSignalRank(),
                signal.getSignalDate()
        );
    }
}
