package com.obigo.demodong.domain.signal.application.usecase;

import com.obigo.demodong.domain.ai.infrastructure.service.AiChatService;
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

    @Value("classpath:prompts/stock-analysis-system.st")
    private Resource systemPromptResource;

    @Value("classpath:prompts/stock-analysis-user.st")
    private Resource userPromptResource;

    @Transactional
    public StockAnalysisResponse execute(String query) {
        MarketType marketType = detectMarketType(query);
        log.info("주식 분석 시작 - query: {}, marketType: {}", query, marketType);

        String rawNews = findCrawler(marketType).crawl(query);
        String systemPrompt = new PromptTemplate(systemPromptResource).render();
        String userPrompt = new PromptTemplate(userPromptResource).render(Map.of(
                "company", query,
                "news", rawNews.isEmpty() ? "최근 뉴스를 찾을 수 없습니다." : rawNews
        ));

        String analysis = aiChatService.getChatResponse(systemPrompt, userPrompt);
        SignalType signalType = saveReport(query, marketType, rawNews, analysis);
        log.info("주식 분석 완료 - query: {}, signal: {}", query, signalType);
        return new StockAnalysisResponse(query, marketType, signalType, analysis);
    }

    public Flux<ServerSentEvent<String>> executeStream(String query) {
        MarketType marketType = detectMarketType(query);
        log.info("주식 분석(SSE) 시작 - query: {}, marketType: {}", query, marketType);

        String rawNews = findCrawler(marketType).crawl(query);
        String systemPrompt = new PromptTemplate(systemPromptResource).render();
        String userPrompt = new PromptTemplate(userPromptResource).render(Map.of(
                "company", query,
                "news", rawNews.isEmpty() ? "최근 뉴스를 찾을 수 없습니다." : rawNews
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
                    SignalType signalType = saveReport(query, marketType, rawNews, analysis);
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
    public SignalType saveReport(String query, MarketType marketType, String rawNews, String analysis) {
        SignalType signalType = parseSignalType(analysis);
        String reason = parseReason(analysis);

        Stock stock = stockRepository.findByTicker(query)
                .orElseGet(() -> stockRepository.save(
                        Stock.builder()
                                .ticker(query)
                                .name(query)
                                .marketType(marketType)
                                .isWatchlist(false)
                                .build()
                ));

        signalReportRepository.save(
                SignalReport.builder()
                        .stock(stock)
                        .signalType(signalType)
                        .reason(reason)
                        .content(analysis)
                        .rawNewsText(rawNews)
                        .sourceType(SourceType.ON_DEMAND)
                        .build()
        );

        return signalType;
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
