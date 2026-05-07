package com.obigo.demodong.domain.stock.infrastructure.crawler;

import com.obigo.demodong.domain.stock.domain.enums.MarketType;

public interface NewsCrawlerStrategy {
    MarketType getMarketType();

    String crawl(String ticker);
}
