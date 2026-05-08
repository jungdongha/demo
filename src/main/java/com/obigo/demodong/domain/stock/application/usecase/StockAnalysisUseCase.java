package com.obigo.demodong.domain.stock.application.usecase;

import com.obigo.demodong.domain.ai.infrastructure.service.AiChatService;
import com.obigo.demodong.domain.stock.application.dto.response.StockAnalysisResponse;
import com.obigo.demodong.domain.stock.domain.entity.SignalReport;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.enums.SignalType;
import com.obigo.demodong.domain.stock.domain.enums.SourceType;
import com.obigo.demodong.domain.stock.domain.repository.SignalReportRepository;
import com.obigo.demodong.domain.stock.domain.repository.StockRepository;
import com.obigo.demodong.domain.stock.infrastructure.crawler.NewsCrawlerStrategy;
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

    // ★ @Value로 classpath의 .st 파일을 Resource로 주입
    //   system: 역할 + 규칙 (정적), user: 뉴스 데이터 (동적)
    @Value("classpath:prompts/stock-analysis-system.st")
    private Resource systemPromptResource;

    @Value("classpath:prompts/stock-analysis-user.st")
    private Resource userPromptResource;

    @Transactional
    public StockAnalysisResponse execute(String query) {
        // 1. 마켓 타입 자동 감지: 한글 포함 → KOR, 영문 → USA
        MarketType marketType = detectMarketType(query);
        log.info("주식 분석 시작 - query: {}, marketType: {}", query, marketType);

        // 2. 마켓 타입에 맞는 크롤러 선택
        // ★ List<NewsCrawlerStrategy>는 Spring이 모든 구현체를 자동 주입
        //   → NaverFinanceCrawler(KOR), YahooFinanceCrawler(USA) 중 하나 선택
        NewsCrawlerStrategy crawler = crawlers.stream()
                .filter(c -> c.getMarketType() == marketType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지원하지 않는 마켓 타입: " + marketType));

        // 3. 뉴스 크롤링 (raw_news_text로 보존)
        String rawNews = crawler.crawl(query);

        // 4. PromptTemplate으로 system/user 프롬프트 완성
        // ★ system은 변수 없음(정적), user는 {company}/{news} 치환
        String systemPrompt = new PromptTemplate(systemPromptResource).render();
        String userPrompt = new PromptTemplate(userPromptResource).render(Map.of(
                "company", query,
                "news", rawNews.isEmpty() ? "최근 뉴스를 찾을 수 없습니다." : rawNews
        ));

        // 5. AI 분석 호출
        String analysis = aiChatService.getChatResponse(systemPrompt, userPrompt);

        // 6. AI 응답 저장
        SignalType signalType = saveReport(query, marketType, rawNews, analysis);
        log.info("주식 분석 완료 - query:{}, signal :{}", query, signalType);
        return new StockAnalysisResponse(query, marketType, signalType, analysis);
    }

    public Flux<ServerSentEvent<String>> executeStream(String query) {
        MarketType marketType = detectMarketType(query);
        log.info("sse 주식분석 시작- query: {}, marketType: {}", query, marketType);

        NewsCrawlerStrategy crawler = crawlers.stream()
                .filter(c -> c.getMarketType() == marketType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지원하지 않는 마켓 타입: " + marketType));

        String rawNews = crawler.crawl(query);
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

    private SignalType saveReport(String query, MarketType marketType, String rawNews, String analysis) {
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

    // ★ 한글 정규식 [가-힣]: 유니코드 범위로 한글 문자 포함 여부 판단
    private MarketType detectMarketType(String query) {
        return query.matches(".*[가-힣].*") ? MarketType.KOR : MarketType.USA;
    }

    // ★ AI 응답에서 [BUY]/[HOLD]/[SELL] 추출
    //   프롬프트에서 명시적으로 [] 포맷을 요구했으므로 contains()로 파싱 가능
    private SignalType parseSignalType(String content) {
        if (content.contains("[BUY]")) return SignalType.BUY;
        if (content.contains("[SELL]")) return SignalType.SELL;
        if (content.contains("[HOLD]")) return SignalType.HOLD;
        log.warn("signal_type 파싱 실패, HOLD로 기본값 처리");
        return SignalType.HOLD;
    }

    // ★ "판단 근거" 섹션 이후 "- "로 시작하는 줄들을 추출
    //   "---" 구분선을 만나면 추출 종료
    private String parseReason(String content) {
        StringBuilder sb = new StringBuilder();
        boolean capture = false;
        for (String line : content.split("\n")) {
            if (line.contains("판단 근거")) {
                capture = true;
                continue;
            }
            if (capture && line.trim().startsWith("-") && !line.trim().startsWith("---")) {
                sb.append(line.trim()).append("\n");
            }
            if (capture && line.trim().startsWith("---")) {
                break;
            }
        }
        return sb.toString().trim();
    }
}
