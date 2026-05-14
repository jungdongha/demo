package com.obigo.demodong.domain.signal.application.usecase;

import com.obigo.demodong.domain.ai.infrastructure.service.AiChatService;
import com.obigo.demodong.domain.portfolio.domain.service.PortfolioReader;
import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.price.domain.model.DisclosureItem;
import com.obigo.demodong.domain.price.domain.port.CorporateDisclosurePort;
import com.obigo.demodong.domain.price.domain.port.StockPricePort;
import com.obigo.demodong.domain.price.domain.service.PriceAnalysisHelper;
import com.obigo.demodong.domain.price.infrastructure.dart.DartCorpCodeMapper;
import com.obigo.demodong.domain.signal.application.dto.response.SignalHistoryResponse;
import com.obigo.demodong.domain.signal.application.dto.response.StockAnalysisResponse;
import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.enums.SignalType;
import com.obigo.demodong.domain.signal.domain.enums.SourceType;
import com.obigo.demodong.domain.signal.domain.service.SignalReportReader;
import com.obigo.demodong.domain.signal.domain.service.SignalReportWriter;
import com.obigo.demodong.domain.signal.infrastructure.crawler.NewsCrawlerStrategy;
import com.obigo.demodong.domain.stock.application.exception.StockErrorCode;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import com.obigo.demodong.domain.stock.domain.service.StockWriter;
import com.obigo.demodong.global.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockAnalysisUseCase {

    private final List<NewsCrawlerStrategy> crawlers;
    private final AiChatService aiChatService;
    private final StockReader stockReader;
    private final StockWriter stockWriter;
    private final SignalReportWriter signalReportWriter;
    private final SignalReportReader signalReportReader;
    private final StockPricePort stockPricePort;
    private final PortfolioReader portfolioReader;
    private final CorporateDisclosurePort corporateDisclosurePort;
    private final DartCorpCodeMapper dartCorpCodeMapper;
    private final PriceAnalysisHelper priceAnalysisHelper;

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
        String ticker = resolveQuery(query);
        MarketType marketType = detectMarketType(ticker);
        log.info("주식 분석 시작 - query: {}, ticker: {}, marketType: {}, sourceType: {}", query, ticker, marketType, sourceType);

        Stock stock = findOrCreateStock(ticker, query, marketType);
        String rawNews = findCrawler(marketType).crawl(query);
        String priceData = fetchPriceContext(stock);
        String portfolioContext = buildPortfolioContext(stock);
        String disclosureData = fetchDisclosureData(stock);

        String systemPrompt = new PromptTemplate(systemPromptResource).render();
        String userPrompt = new PromptTemplate(userPromptResource).render(Map.of(
                "company", stock.getName(),
                "sector", stock.getSector() != null ? stock.getSector() : "미분류",
                "news", rawNews.isEmpty() ? "최근 뉴스를 찾을 수 없습니다. 주가 흐름과 공시 데이터를 중심으로 판단하라." : rawNews,
                "priceData", priceData,
                "disclosureData", disclosureData.isBlank() ? "최근 공시 없음" : disclosureData,
                "portfolioContext", portfolioContext
        ));

        String analysis = aiChatService.getChatResponse(systemPrompt, userPrompt);
        String reason = parseReason(analysis);
        SignalType signalType = saveReport(stock, rawNews, analysis, sourceType);
        log.info("주식 분석 완료 - query: {}, signal: {}", query, signalType);
        return StockAnalysisResponse.of(stock.getName(), marketType, signalType, analysis, reason);
    }

    @Transactional
    public Flux<ServerSentEvent<String>> executeStream(String query) {
        return Flux.defer(() -> {
            String ticker = resolveQuery(query);
            MarketType marketType = detectMarketType(ticker);
            log.info("주식 분석(SSE) 시작 - query: {}, ticker: {}, marketType: {}", query, ticker, marketType);

            Stock stock = findOrCreateStock(ticker, query, marketType);
            String rawNews = findCrawler(marketType).crawl(query);
            String priceData = fetchPriceContext(stock);
            String portfolioContext = buildPortfolioContext(stock);
            String disclosureData = fetchDisclosureData(stock);

            String systemPrompt = new PromptTemplate(systemPromptResource).render();
            String userPrompt = new PromptTemplate(userPromptResource).render(Map.of(
                    "company", stock.getName(),
                    "sector", stock.getSector() != null ? stock.getSector() : "미분류",
                    "news", rawNews.isEmpty() ? "최근 뉴스를 찾을 수 없습니다. 주가 흐름과 공시 데이터를 중심으로 판단하라." : rawNews,
                    "priceData", priceData,
                    "disclosureData", disclosureData.isBlank() ? "최근 공시 없음" : disclosureData,
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
                    }));
        }).onErrorResume(e -> {
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

    public Page<SignalHistoryResponse> getHistoryByTicker(String ticker, Pageable pageable) {
        return signalReportReader.findByTicker(ticker, pageable)
                .map(SignalHistoryResponse::from);
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

    /**
     * 입력값을 실제 티커 코드로 변환.
     * - 6자리 숫자 → KOR 티커 (그대로 반환)
     * - 영문 1~10자 → USA 티커 (대문자 변환)
     * - 한글 포함 → DART 회사명 검색 → 미발견 시 STOCK_NOT_FOUND
     */
    private String resolveQuery(String query) {
        String trimmed = query.trim();
        if (trimmed.matches("\\d{6}")) return trimmed;
        if (trimmed.matches("[A-Za-z.\\-]{1,10}")) return trimmed.toUpperCase();
        if (trimmed.matches(".*[가-힣].*")) {
            return dartCorpCodeMapper.resolveTickerByName(trimmed)
                    .orElseThrow(() -> {
                        log.warn("종목 검색 실패 - query: {}", trimmed);
                        return new ApplicationException(StockErrorCode.STOCK_NOT_FOUND);
                    });
        }
        throw new ApplicationException(StockErrorCode.STOCK_NOT_FOUND);
    }

    /**
     * 티커로 Stock 조회. 없으면 신규 생성 (displayName을 종목명으로 사용).
     */
    private Stock findOrCreateStock(String ticker, String displayName, MarketType marketType) {
        return stockReader.findByTicker(ticker)
                .orElseGet(() -> {
                    String dartCorpCode = marketType == MarketType.KOR
                            ? dartCorpCodeMapper.resolveCorpCodeByTicker(ticker).orElse(null)
                            : null;
                    return stockWriter.save(
                            Stock.builder()
                                    .ticker(ticker)
                                    .name(displayName)
                                    .marketType(marketType)
                                    .dartCorpCode(dartCorpCode)
                                    .isWatchlist(false)
                                    .build()
                    );
                });
    }

    private String fetchPriceContext(Stock stock) {
        try {
            List<PriceSnapshot> snapshots = stockPricePort.fetchMonthlyPrices(stock);
            Optional<BigDecimal[]> w52 = stockPricePort.fetch52WeekRange(stock);
            return priceAnalysisHelper.buildPriceContext(snapshots, w52);
        } catch (Exception e) {
            log.warn("주가 데이터 조회 실패 - ticker: {}", stock.getTicker());
            return "주가 데이터 조회 실패 — 주가 분석 없이 뉴스·공시만으로 판단하라.";
        }
    }

    private String fetchDisclosureData(Stock stock) {
        if (stock.getMarketType() != MarketType.KOR) return "해외 종목 — 공시 데이터 미지원";

        if (stock.getDartCorpCode() == null || stock.getDartCorpCode().isBlank()) {
            log.warn("DART corp_code 미매핑 - ticker: {}", stock.getTicker());
            throw new ApplicationException(StockErrorCode.DART_CORP_CODE_NOT_MAPPED);
        }

        try {
            List<DisclosureItem> items = corporateDisclosurePort.fetchRecentDisclosures(stock.getDartCorpCode(), 5);
            return corporateDisclosurePort.format(items);
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("공시 데이터 조회 실패 - ticker: {}, error: {}", stock.getTicker(), e.getMessage());
            return "공시 정보 없음 (DART API 일시 오류)";
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

    private NewsCrawlerStrategy findCrawler(MarketType marketType) {
        return crawlers.stream()
                .filter(c -> c.getMarketType() == marketType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지원하지 않는 마켓 타입: " + marketType));
    }

    private MarketType detectMarketType(String ticker) {
        return ticker.matches("\\d{6}") ? MarketType.KOR : MarketType.USA;
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
            if (line.contains("핵심 요약")) { capture = true; continue; }
            if (capture && line.trim().startsWith("-") && !line.trim().startsWith("---"))
                sb.append(line.trim()).append("\n");
            if (capture && line.trim().startsWith("---")) break;
        }
        return sb.toString().trim();
    }
}
