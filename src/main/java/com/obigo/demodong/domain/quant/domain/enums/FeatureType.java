package com.obigo.demodong.domain.quant.domain.enums;

public enum FeatureType {
    VOLUME_RATIO_5D,          // 거래량 배수 (현재 / 5일 평균)
    PRICE_MOMENTUM_5D,        // 5일 가격 모멘텀 (%)
    NEWS_FRESHNESS,           // 뉴스 신선도 점수 (0~100)
    MARKET_REGIME,            // 시장 국면 보정 계수 (-10 ~ +10)
    VALUATION_SCORE,          // P/E·P/B 저평가도 (0~100, 낮을수록 매력)
    TARGET_PRICE_UPSIDE,      // 증권사 목표주가 대비 상승 여력 (%, 0~100 정규화)
    SECTOR_RELATIVE_STRENGTH  // 유니버스 내 동일 섹터 대비 상대 강도 (0~100)
}
