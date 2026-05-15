package com.obigo.demodong.domain.quant.domain.port;

import com.obigo.demodong.domain.quant.domain.model.DailyQuote;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;

import java.util.List;

/**
 * Quant 전용 외부 데이터 수집 인터페이스 (DIP).
 * Core의 StockPricePort와 독립적으로 유지한다.
 */
public interface QuantDataPort {

    /**
     * 최근 N일 일별 주가(종가, 거래량) 조회.
     * KOR: KIS API / USA: Alpha Vantage (MVP에서는 Stub)
     */
    List<DailyQuote> fetchRecentPrices(String ticker, MarketType market, int days);

    /**
     * 시장 지수 5일 변동률 (%) 조회.
     * KOR: KOSPI / USA: 고정값 0.0 (MVP Stub)
     */
    double fetchIndexReturn5d(MarketType market);
}
