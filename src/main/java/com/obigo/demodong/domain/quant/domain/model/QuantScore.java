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
        Map<FeatureType, Double> featureRawValues,  // 각 Feature 원시값 (스냅샷·디버깅용)
        double volumeRatio5d,
        double priceMomentum5d,
        double newsFreshness,
        double valuationScore,          // 밸류에이션 정규화 점수 (0~100)
        double targetPriceUpsideScore,  // 목표주가 상승여력 정규화 점수 (0~100)
        double sectorRelativeScore,     // 섹터 상대 강도 정규화 점수 (0~100)
        QuantFundamentalData fundamentals,  // P/E·P/B·목표주가 원시값 (스냅샷 저장용, nullable)
        MarketRegime marketRegime,
        boolean excluded,
        String excludeReason,
        LocalDateTime calculatedAt
) {
    public static QuantScore excluded(String ticker, String stockName, MarketType marketType,
                                      MarketRegime regime, String reason) {
        return new QuantScore(ticker, stockName, marketType, 0.0,
                Map.of(), 0.0, 0.0, 0.0, 50.0, 50.0, 50.0,
                null, regime, true, reason, LocalDateTime.now());
    }
}
