package com.obigo.demodong.domain.stock.infrastructure.crawler;

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

        // ★ .defaultIfEmpty() : Mono가 비어있을 때 null 대신 빈 응답 반환
        //   block() 이후 null 체크 불필요해짐
        NaverNewsResponse response = naverWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("query", query + " 주식")
                        .queryParam("display", 10)
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

        List<NaverNewsItem> items = response.items();

        // ★ filter 후 List로 수집 → IntStream.range()로 인덱스 번호 부여
        //   AtomicInteger 없이 순번 매기는 패턴
        String result = IntStream.range(0, items.size())
                .mapToObj(i -> {
                    NaverNewsItem item = items.get(i);
                    // ★ HTML 태그 제거: "<b>삼성전자</b>" → "삼성전자", "&quot;" → "\""
                    String title = Jsoup.parse(item.title()).text();
                    String description = Jsoup.parse(item.description()).text();
                    String date = formatDate(item.pubDate());
                    return String.format("[%d] %s\n제목: %s\n내용: %s",
                            i + 1, date, title, description);
                })
                .collect(Collectors.joining("\n\n"));

        log.info("Naver crawl() 완료 - {}건", items.size());
        return result;
    }

    // ★ RFC 1123 날짜 포맷 파싱: "Wed, 07 May 2026 16:00:00 +0900" → "2026-05-07"
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
