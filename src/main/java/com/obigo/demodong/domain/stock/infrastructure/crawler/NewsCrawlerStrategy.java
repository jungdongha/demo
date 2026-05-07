package com.obigo.demodong.domain.stock.infrastructure.crawler;

import com.obigo.demodong.domain.stock.domain.enums.MarketType;

public interface NewsCrawlerStrategy {
    MarketType getMarketType();

    // ★ KOR: 회사명("삼성전자"), USA: 티커코드("AAPL") 전달
    String crawl(String query);
}
