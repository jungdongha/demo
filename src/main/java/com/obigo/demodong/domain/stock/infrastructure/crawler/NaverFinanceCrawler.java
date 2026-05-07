package com.obigo.demodong.domain.stock.infrastructure.crawler;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverFinanceCrawler implements NewsCrawlerStrategy {
    private final WebClient naverWebClient;

    @Override
    public MarketType getMarketType() {
        return MarketType.KOR;
    }

    @Override
    public String crawl(String ticker) {
        log.info("NaverCrawl crawl() 시작");
        NaverNewsResponse response = naverWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("query", ticker + " 주식")
                        .queryParam("display", 10)
                        .queryParam("sort", "date")
                        .build())
                .retrieve()
                .bodyToMono(NaverNewsResponse.class)
                .block();

        if (response == null || response.items() == null || response.items().isEmpty()) {
            log.warn("Naver Crawl() 결과 없음 - ticker: {}", ticker);
            return "";
        }

        String result = response.items().stream()
                .map(item -> item.title() + "\n" + item.description())
                .collect(Collectors.joining("\n\n"));

        log.info("Naver crawl() 완료 - {}건", response.items().size());
        return result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NaverNewsResponse(List<NaverNewsItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NaverNewsItem(String title, String description, String link, String pubDate) {}
}
