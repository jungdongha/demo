package com.obigo.demodong.domain.signal.application.dto.response;

import com.obigo.demodong.domain.signal.domain.enums.SignalType;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;

public record StockAnalysisResponse(
        String company,
        MarketType marketType,
        SignalType signalType,
        String analysis,
        String reason
) {
    public static StockAnalysisResponse of(String company, MarketType marketType, SignalType signalType, String analysis, String reason) {
        return new StockAnalysisResponse(company, marketType, signalType, analysis, reason);
    }
}
