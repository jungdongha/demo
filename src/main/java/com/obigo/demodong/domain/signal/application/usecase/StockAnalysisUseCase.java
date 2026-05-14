package com.obigo.demodong.domain.signal.application.usecase;

import com.obigo.demodong.domain.ai.infrastructure.service.AiChatService;
import com.obigo.demodong.domain.portfolio.domain.repository.PortfolioDetailRepository;
import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.price.infrastructure.StockPriceFetcher;
import com.obigo.demodong.domain.signal.application.dto.response.StockAnalysisResponse;
import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.enums.SignalType;
import com.obigo.demodong.domain.signal.domain.enums.SourceType;
import com.obigo.demodong.domain.signal.domain.repository.SignalReportRepository;
import com.obigo.demodong.domain.signal.infrastructure.crawler.NewsCrawlerStrategy;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAnalysisUseCase {

    private final List<NewsCrawlerStrategy> crawlers;
    private final AiChatService aiChatService;
    private final StockRepository stockRepository;
    private final SignalReportRepository signalReportRepository;
    private final StockPriceFetcher stockPriceFetcher;
    private final PortfolioDetailRepository portfolioDetailRepository;

    @Value("classpath:prompts/stock-analysis-system.st")
    private Resource systemPromptResource;

    @Value("classpath:prompts/stock-analysis-user.st")
    private Resource userPromptResource;

    @Transactional
    public StockAnalysisResponse execute(String query) {
        MarketType marketType = detectMarketType(query);
        log.info("주식 분석 시작 - query: {}, marketType: {}", query, marketType);

        Stock stock = findOrCreateStock(query, marketType);
        String rawNews = findCrawler(marketType).crawl(query);
        String priceData = fetchPriceData(stock);
        String portfolioContext = buildPortfolioContext(stock);

        String systemPrompt = new PromptTemplate(systemPromptResource).render();
        String userPrompt = new PromptTemplate(userPromptResource).render(Map.of(
                "company", query,
                "news", rawNews.isEmpty() ? "최근 뉴스를 찾을 수 없습니다." : rawNews,
                "priceData", priceData,
                "portfolioContext", portfolioContext
        ));

        String analysis = aiChatService.getChatResponse(systemPrompt, userPrompt);
        SignalType signalType = saveReport(stock, rawNews, analysis, SourceType.ON_DEMAND);
        log.info("주식 분석 완료 - query: {}, signal: {}", query, signalType);
        return new StockAnalysisResponse(query, marketType, signalType, analysis);
    }

    @Transactional
    public StockAnalysisResponse executeScheduled(String ticker) {
        MarketType marketType = detectMarketType(ticker);
        log.info("스케줄 분석 시작 - ticker: {}, marketType: {}", ticker, marketType);

        Stock stock = findOrCreateStock(ticker, marketType);
        String rawNews = findCrawler(marketType).crawl(ticker);
        String priceData = fetchPriceData(stock);
        String portfolioContext = buildPortfolioContext(stock);

        String systemPrompt = new PromptTemplate(systemPromptResource).render();
        String userPrompt = new PromptTemplate(userPromptResource).render(Map.of(
                "company", ticker,
                "news", rawNews.isEmpty() ? "최근 뉴스를 찾을 수 없습니다." : rawNews,
                "priceData", priceData,
                "portfolioContext", portfolioContext
        ));

        String analysis = aiChatService.getChatResponse(systemPrompt, userPrompt);
        SignalType signalType = saveReport(stock, rawNews, analysis, SourceType.SCHEDULED);
        log.info("스케줄 분석 완료 - ticker: {}, signal: {}", ticker, signalType);
        return new StockAnalysisResponse(ticker, marketType, signalType, analysis);
    }

    public Flux<ServerSentEvent<String>> executeStream(String query) {
        MarketType marketType = detectMarketType(query);
        log.info("주식 분석(SSE) 시작 - query: {}, marketType: {}", query, marketType);

        Stock stock = findOrCreateStock(query, marketType);
        String rawNews = findCrawler(marketType).crawl(query);
        String priceData = fetchPriceData(stock);
        String portfolioContext = buildPortfolioContext(stock);

        String systemPrompt = new PromptTemplate(systemPromptResource).render();
        String userPrompt = new PromptTemplate(userPromptResource).render(Map.of(
                "company", query,
                "news", rawNews.isEmpty() ? "최근 뉴스를 찾을 수 없습니다." : rawNews,
                "priceData", priceData,
                "portfolioContext", portfolioContext
        ));

        StringBuilder fullContent = new StringBuilder();

        return aiChatService.streamChatResponse(systemPrompt, userPrompt)
                .doOnNext(fullContent::append)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(chunk)
                        .build())
                .concatWith(Flux.defer(() -> {
                    String analysis = fullContent.toString();
                    SignalType signalType = saveReport(stock, rawNews, analysis, SourceType.ON_DEMAND);
                    log.info("주식 분석(SSE) 완료 - query: {}, signal: {}", query, signalType);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("done")
                            .data(signalType.name())
                            .build());
                }))
                .onErrorResume(e -> {
                    log.error("주식 분석(SSE) 오류 - query: {}, error: {}", query, e.getMessage());
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(e.getMessage())
                            .build());
                });
    }

    @Transactional
    public SignalType saveReport(Stock stock, String rawNews, String analysis, SourceType sourceType) {
        SignalType signalType = parseSignalType(analysis);
        String reason = parseReason(analysis);
        signalReportRepository.save(
                SignalReport.builder()
                        .stock(stock)
                        .signalType(signalType)
                        .reason(reason)
                        .content(analysis)
                        .rawNewsText(rawNews)
                        .sourceType(sourceType)
                        .build()
        );
        return signalType;
    }

    private Stock findOrCreateStock(String query, MarketType marketType) {
        return stockRepository.findByTicker(query)
                .orElseGet(() -> stockRepository.save(
                        Stock.builder()
                                .ticker(query).name(query)
                                .marketType(marketType).isWatchlist(false)
                                .build()
                ));
    }

    private String fetchPriceData(Stock stock) {
        if (stock.getMarketType() != MarketType.USA) return "주가 데이터 없음";
        try {
            List<PriceSnapshot> snapshots = stockPriceFetcher.fetchMonthlyPrices(stock);
            return stockPriceFetcher.formatPriceHistory(snapshots);
        } catch (Exception e) {
            log.warn("주가 데이터 조회 실패 - ticker: {}", stock.getTicker());
            return "주가 데이터 조회 실패";
        }
    }

    private String buildPortfolioContext(Stock stock) {
        return portfolioDetailRepository
                .findByStock_TickerAndDeletedFalse(stock.getTicker())
                .map(detail -> {
                    BigDecimal currentPrice = stockPriceFetcher.fetchCurrentPrice(stock);
                    if (currentPrice == null) return "";
                    BigDecimal profitRate = currentPrice.subtract(detail.getAvgPrice())
                            .divide(detail.getAvgPrice(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);
                    return "[보유 현황]\n평균매수가: " + detail.getAvgPrice()
                            + " / 현재가: " + currentPrice
                            + " / 수익률: " + profitRate + "%";
                })
                .orElse("");
    }

    private NewsCrawlerStrategy findCrawler(MarketType marketType) {
        return crawlers.stream()
                .filter(c -> c.getMarketType() == marketType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지원하지 않는 마켓 타입: " + marketType));
    }

    private MarketType detectMarketType(String query) {
        return query.matches(".*[가-힣].*") ? MarketType.KOR : MarketType.USA;
    }

    private SignalType parseSignalType(String content) {
        if (content.contains("[BUY]")) return SignalType.BUY;
        if (content.contains("[SELL]")) return SignalType.SELL;
        if (content.contains("[HOLD]")) return SignalType.HOLD;
        log.warn("signal_type 파싱 실패, HOLD로 기본값 처리");
        return SignalType.HOLD;
    }

    private String parseReason(String content) {
        StringBuilder sb = new StringBuilder();
        boolean capture = false;
        for (String line : content.split("\n")) {
            if (line.contains("판단 근거")) { capture = true; continue; }
            if (capture && line.trim().startsWith("-") && !line.trim().startsWith("---"))
                sb.append(line.trim()).append("\n");
            if (capture && line.trim().startsWith("---")) break;
        }
        return sb.toString().trim();
    }
}
