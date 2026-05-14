package com.obigo.demodong.domain.signal.infrastructure.crawler;

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
import java.util.List;
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
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();

            Elements items = doc.select("item");

            if (items.isEmpty()) {
                log.warn("[YahooFinanceCrawler] 뉴스 없음 - ticker: {}", query);
                return "";
            }

            ZonedDateTime twentyFourHoursAgo = ZonedDateTime.now().minusHours(24);

            List<org.jsoup.nodes.Element> filteredItems = items.stream()
                    .filter(item -> {
                        try {
                            String pubDateStr = item.select("pubDate").text();
                            ZonedDateTime pubDate = ZonedDateTime.parse(pubDateStr, DateTimeFormatter.RFC_1123_DATE_TIME);
                            return pubDate.isAfter(twentyFourHoursAgo);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .limit(10)
                    .toList();

            if (filteredItems.isEmpty()) {
                log.warn("[YahooFinanceCrawler] 24시간 이내 뉴스 없음 - ticker: {}", query);
                return "";
            }

            String result = IntStream.range(0, filteredItems.size())
                    .mapToObj(i -> {
                        String title = filteredItems.get(i).select("title").text();
                        String description = filteredItems.get(i).select("description").text();
                        String date = formatDate(filteredItems.get(i).select("pubDate").text());
                        return String.format("[%d] %s\n제목: %s\n내용: %s", i + 1, date, title, description);
                    })
                    .collect(Collectors.joining("\n\n"));

            log.info("[YahooFinanceCrawler] 완료 - {}건", filteredItems.size());
            return result;

        } catch (IOException e) {
            log.error("[YahooFinanceCrawler] 크롤링 실패 - ticker: {}, error: {}", query, e.getMessage());
            throw new RuntimeException("야후 파이낸스 크롤링 실패: " + e.getMessage());
        }
    }

    private String formatDate(String pubDate) {
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return pubDate;
        }
    }
}
