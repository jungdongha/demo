package com.obigo.demodong.domain.quant.domain.model;

import com.obigo.demodong.domain.quant.domain.enums.FeatureType;
import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Quant Scoring Engine이 산출한 종목별 종합 점수.
 * RiskFilterService가 EXCLUDE 판정을 하면 excluded=true로 설정된다.
 */
public record QuantScore(
        String ticker,
        String stockName,
        MarketType marketType,
        double totalScore,
        Map<FeatureType, Double> featureRawValues,  // 원시값 저장 (스냅샷용)
        double volumeRatio5d,
        double priceMomentum5d,
        double newsFreshness,
        MarketRegime marketRegime,
        boolean excluded,
        String excludeReason,
        LocalDateTime calculatedAt
) {
    public static QuantScore excluded(String ticker, String stockName, MarketType marketType,
                                      MarketRegime regime, String reason) {
        return new QuantScore(ticker, stockName, marketType, 0.0,
                Map.of(), 0.0, 0.0, 0.0, regime, true, reason, LocalDateTime.now());
    }
}
