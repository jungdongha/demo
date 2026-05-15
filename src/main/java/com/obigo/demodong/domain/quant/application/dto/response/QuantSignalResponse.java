package com.obigo.demodong.domain.quant.application.dto.response;

import com.obigo.demodong.domain.quant.domain.entity.QuantFeatureSnapshot;
import com.obigo.demodong.domain.quant.domain.entity.QuantSignal;
import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QuantSignalResponse(
        Long signalId,
        String ticker,
        String stockName,
        MarketType marketType,
        BigDecimal totalScore,
        int signalRank,
        MarketRegime marketRegime,
        String llmReport,
        String disclaimer,
        LocalDate signalDate,
        QuantFeatureSnapshotResponse featureSnapshot
) {
    public static QuantSignalResponse from(QuantSignal signal, QuantFeatureSnapshot snapshot) {
        return new QuantSignalResponse(
                signal.getId(),
                signal.getTicker(),
                signal.getStockName(),
                signal.getMarketType(),
                signal.getTotalScore(),
                signal.getSignalRank(),
                signal.getMarketRegime(),
                signal.getLlmReport(),
                signal.getDisclaimer(),
                signal.getSignalDate(),
                snapshot != null ? QuantFeatureSnapshotResponse.from(snapshot) : null
        );
    }

    public static QuantSignalResponse from(QuantSignal signal) {
        return from(signal, null);
    }
}
