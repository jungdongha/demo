package com.obigo.demodong.domain.quant.domain.enums;

public enum FeatureType {
    VOLUME_RATIO_5D,     // 거래량 배수 (현재 / 5일 평균)
    PRICE_MOMENTUM_5D,   // 5일 가격 모멘텀 (%)
    NEWS_FRESHNESS,      // 뉴스 신선도 점수 (0~100)
    MARKET_REGIME        // 시장 국면 보정 계수 (-10 ~ +10)
}
