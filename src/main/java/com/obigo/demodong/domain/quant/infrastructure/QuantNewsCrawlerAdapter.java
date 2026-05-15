package com.obigo.demodong.domain.quant.infrastructure;

import com.obigo.demodong.domain.quant.domain.port.QuantNewsPort;
import com.obigo.demodong.domain.signal.infrastructure.crawler.NewsCrawlerStrategy;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * QuantNewsPort 구현체 — 기존 NewsCrawlerStrategy를 Quant 도메인용으로 감싼다.
 * Module Guard 준수: Quant 도메인은 NewsCrawlerStrategy를 직접 의존하지 않고 이 Adapter를 통한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuantNewsCrawlerAdapter implements QuantNewsPort {

    private final List<NewsCrawlerStrategy> crawlers;

    @Override
    public List<String> fetchNews(String query, MarketType market) {
        try {
            NewsCrawlerStrategy crawler = crawlers.stream()
                    .filter(c -> c.getMarketType() == market)
                    .findFirst()
                    .orElse(null);

            if (crawler == null) {
                log.warn("[QuantNews] 지원하지 않는 마켓 타입: {}", market);
                return Collections.emptyList();
            }

            String raw = crawler.crawl(query);
            if (raw == null || raw.isBlank()) return Collections.emptyList();

            return Arrays.stream(raw.split("\n"))
                    .filter(line -> !line.isBlank())
                    .toList();

        } catch (Exception e) {
            log.warn("[QuantNews] 뉴스 조회 실패 - query={}, market={}, error={}", query, market, e.getMessage());
            return Collections.emptyList();
        }
    }
}
