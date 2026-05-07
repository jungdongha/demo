package com.obigo.demodong.domain.stock.infrastructure.crawler;

import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class YahooFinanceCrawler implements NewsCrawlerStrategy {

    @Value("${stock.yahoo.rss-url}")
    private String yahooRssUrl;

    @Override
    public MarketType getMarketType() {
        return MarketType.USA;
    }

    @Override
    public String crawl(String query) {
        String url = yahooRssUrl + query;
        log.info("[YahooFinanceCrawler] 크롤링 시작 - ticker: {}", query);

        try {
            // ★ RSS는 XML 포맷 - Jsoup이 그대로 파싱 가능
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();

            // ★ Elements는 ArrayList<Element>를 상속 → get(i) 바로 사용 가능
            Elements items = doc.select("item");

            if (items.isEmpty()) {
                log.warn("[YahooFinanceCrawler] 뉴스 없음 - ticker: {}", query);
                return "";
            }

            int count = Math.min(items.size(), 10);

            String result = IntStream.range(0, count)
                    .mapToObj(i -> {
                        String title = items.get(i).select("title").text();
                        String description = items.get(i).select("description").text();
                        String date = formatDate(items.get(i).select("pubDate").text());
                        return String.format("[%d] %s\n제목: %s\n내용: %s",
                                i + 1, date, title, description);
                    })
                    .collect(Collectors.joining("\n\n"));

            log.info("[YahooFinanceCrawler] 완료 - {}건", count);
            return result;

        } catch (IOException e) {
            log.error("[YahooFinanceCrawler] 크롤링 실패 - ticker: {}, error: {}", query, e.getMessage());
            throw new RuntimeException("야후 파이낸스 크롤링 실패: " + e.getMessage());
        }
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
}
