package com.obigo.demodong.domain.quant.domain.model;

import com.obigo.demodong.domain.stock.domain.enums.MarketType;

import java.util.List;

/**
 * Feature Calculator에 전달되는 입력 데이터.
 * UseCase에서 외부 데이터를 수집한 후 조립하여 각 Calculator에 전달한다.
 */
public record QuantFeatureInput(
        String ticker,
        String stockName,
        MarketType market,
        List<DailyQuote> priceHistory,          // 최근 5일+ 일별 주가 (최신순)
        List<String> recentNews,                // 최근 뉴스 목록 (신선도 계산용)
        double indexReturn5d,                   // 시장 지수 5일 변동률 (%) — 국면 판단용
        QuantFundamentalData fundamentals,      // P/E·P/B·목표주가 (nullable — KOR만 실제값, USA Stub)
        double sectorAvgMomentum5d              // 유니버스 내 동일 섹터 평균 5일 모멘텀 (%) — 섹터 상대 강도 계산용
) {}
