package com.obigo.demodong.domain.stock.application.dto.response;

import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.enums.SignalType;

public record StockAnalysisResponse(
        String company,
        MarketType marketType,
        SignalType signalType,
        String analysis
) {}
