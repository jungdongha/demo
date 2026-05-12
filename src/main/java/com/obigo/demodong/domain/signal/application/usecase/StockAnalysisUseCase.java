package com.obigo.demodong.domain.signal.application.usecase;

import com.obigo.demodong.domain.ai.infrastructure.service.AiChatService;
import com.obigo.demodong.domain.portfolio.domain.service.PortfolioReader;
import com.obigo.demodong.domain.price.domain.port.StockPricePort;
import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.signal.application.dto.response.SignalHistoryResponse;
import com.obigo.demodong.domain.signal.application.dto.response.StockAnalysisResponse;
import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.enums.SignalType;
import com.obigo.demodong.domain.signal.domain.enums.SourceType;
import com.obigo.demodong.domain.signal.domain.service.SignalReportReader;
import com.obigo.demodong.domain.signal.domain.service.SignalReportWriter;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import com.obigo.demodong.domain.stock.domain.service.StockWriter;
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
@Transactional(readOnly = true)
public class StockAnalysisUseCase {

    private final List<com.obigo.demodong.domain.signal.infrastructure.crawler.NewsCrawlerStrategy> crawlers;
    private final AiChatService aiChatService;
    private final StockReader stockReader;
    private final StockWriter stockWriter;
    private final SignalReportWriter signalReportWriter;
    private final SignalReportReader signalReportReader;
    private final StockPricePort stockPricePort;
    private final PortfolioReader portfolioReader;

    @Value("classpath:prompts/stock-analysis-system.st")
    private Resource systemPromptResource;

    @Value("classpath:prompts/stock-analysis-user.st")
    private Resource userPromptResource;

    @Transactional
    public StockAnalysisResponse execute(String query) {
        return doAnalysis(query, SourceType.ON_DEMAND);
    }

    @Transactional
    public StockAnalysisResponse executeScheduled(String ticker) {
        return doAnalysis(ticker, SourceType.SCHEDULED);
    }

    private StockAnalysisResponse doAnalysis(String query, SourceType sourceType) {
        MarketType marketType = detectMarketType(query);
        log.info("주식 분석 시작 - query: {}, marketType: {}, sourceType: {}", query, marketType, sourceType);

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
        String reason = parseReason(analysis);
        SignalType signalType = saveReport(stock, rawNews, analysis, sourceType);
        log.info("주식 분석 완료 - query: {}, signal: {}", query, signalType);
        return StockAnalysisResponse.of(query, marketType, signalType, analysis, reason);
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

    public List<SignalHistoryResponse> getTodayReports() {
        return signalReportReader.findTodayScheduled().stream()
                .map(SignalHistoryResponse::from)
                .toList();
    }

    public List<SignalHistoryResponse> getHistoryByStockId(Long stockId) {
        return signalReportReader.findByStockId(stockId).stream()
                .map(SignalHistoryResponse::from)
                .toList();
    }

    @Transactional
    public SignalType saveReport(Stock stock, String rawNews, String analysis, SourceType sourceType) {
        SignalType signalType = parseSignalType(analysis);
        String reason = parseReason(analysis);
        signalReportWriter.save(
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
        return stockReader.findByTicker(query)
                .orElseGet(() -> stockWriter.save(
                        Stock.builder()
                                .ticker(query).name(query)
                                .marketType(marketType).isWatchlist(false)
                                .build()
                ));
    }

    private String fetchPriceData(Stock stock) {
        if (stock.getMarketType() != MarketType.USA) return "주가 데이터 없음";
        try {
            List<PriceSnapshot> snapshots = stockPricePort.fetchMonthlyPrices(stock);
            return stockPricePort.formatPriceHistory(snapshots);
        } catch (Exception e) {
            log.warn("주가 데이터 조회 실패 - ticker: {}", stock.getTicker());
            return "주가 데이터 조회 실패";
        }
    }

    private String buildPortfolioContext(Stock stock) {
        return portfolioReader.findByStockTicker(stock.getTicker())
                .map(detail -> {
                    BigDecimal currentPrice = stockPricePort.fetchCurrentPrice(stock);
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

    private com.obigo.demodong.domain.signal.infrastructure.crawler.NewsCrawlerStrategy findCrawler(MarketType marketType) {
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
