package com.obigo.demodong.domain.signal.infrastructure.crawler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public String crawl(String query) {
        log.info("NaverCrawl crawl() 시작 - query: {}", query);

        NaverNewsResponse response = naverWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("query", query + " 주식")
                        .queryParam("display", 100)
                        .queryParam("sort", "date")
                        .build())
                .retrieve()
                .bodyToMono(NaverNewsResponse.class)
                .defaultIfEmpty(new NaverNewsResponse(List.of()))
                .block();

        if (response.items().isEmpty()) {
            log.warn("Naver Crawl() 결과 없음 - query: {}", query);
            return "";
        }

        ZonedDateTime twentyFourHoursAgo = ZonedDateTime.now().minusHours(24);

        List<NaverNewsItem> filteredItems = response.items().stream()
                .filter(item -> {
                    try {
                        ZonedDateTime pubDate = ZonedDateTime.parse(item.pubDate(), DateTimeFormatter.RFC_1123_DATE_TIME);
                        return pubDate.isAfter(twentyFourHoursAgo);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .limit(10)
                .toList();

        if (filteredItems.isEmpty()) {
            log.warn("Naver Crawl() 24시간 이내 뉴스 없음 - query: {}", query);
            return "";
        }

        String result = IntStream.range(0, filteredItems.size())
                .mapToObj(i -> {
                    NaverNewsItem item = filteredItems.get(i);
                    String title = Jsoup.parse(item.title()).text();
                    String description = Jsoup.parse(item.description()).text();
                    String date = formatDate(item.pubDate());
                    return String.format("[%d] %s\n제목: %s\n내용: %s", i + 1, date, title, description);
                })
                .collect(Collectors.joining("\n\n"));

        log.info("Naver crawl() 완료 - {}건", filteredItems.size());
        return result;
    }

    private String formatDate(String pubDate) {
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return pubDate;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NaverNewsResponse(List<NaverNewsItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NaverNewsItem(String title, String description, String link, String pubDate) {}
}
