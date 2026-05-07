package com.obigo.demodong.domain.stock.infrastructure.crawler;

import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Collectors;

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
    public String crawl(String ticker) {
        String url = yahooRssUrl + ticker;
        log.info("[YahooFinanceCrawler] 크롤링 시작 - ticker: {}, url: {}", ticker, url);

        try {
            // ★ RSS는 XML 포맷이라 Jsoup이 그대로 파싱 가능
            //   HTML이 아닌 XML도 Jsoup.connect().get()으로 가져올 수 있음
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();

            // ★ RSS XML 구조:
            //   <channel>
            //     <item>
            //       <title>뉴스 제목</title>
            //       <description>요약</description>
            //     </item>
            //   </channel>
            Elements items = doc.select("item");

            if (items.isEmpty()) {
                log.warn("[YahooFinanceCrawler] 뉴스 없음 - ticker: {}", ticker);
                return "";
            }

            String result = items.stream()
                    .map(item -> item.select("title").text()
                            + "\n"
                            + item.select("description").text())
                    .collect(Collectors.joining("\n\n"));

            log.info("[YahooFinanceCrawler] 완료 - {}건", items.size());
            return result;

        } catch (IOException e) {
            log.error("[YahooFinanceCrawler] 크롤링 실패 - ticker: {}, error: {}", ticker, e.getMessage());
            throw new RuntimeException("야후 파이낸스 크롤링 실패: " + e.getMessage());
        }
    }
}
