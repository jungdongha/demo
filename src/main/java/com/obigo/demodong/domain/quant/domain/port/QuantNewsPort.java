package com.obigo.demodong.domain.quant.domain.port;

import com.obigo.demodong.domain.stock.domain.enums.MarketType;

import java.util.List;

/**
 * Quant 뉴스 수집 인터페이스.
 * Core의 NewsCrawlerStrategy를 직접 의존하지 않고, Adapter로 감싼다 (Module Guard 준수).
 */
public interface QuantNewsPort {

    /**
     * 해당 종목의 최근 뉴스 목록 반환.
     * KOR: 회사명으로 검색 / USA: 티커로 검색
     */
    List<String> fetchNews(String query, MarketType market);
}
