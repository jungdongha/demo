package com.obigo.demodong.domain.signal.application.scheduler;

import com.obigo.demodong.domain.portfolio.domain.entity.PortfolioDetail;
import com.obigo.demodong.domain.portfolio.domain.repository.PortfolioDetailRepository;
import com.obigo.demodong.domain.signal.application.dto.response.StockAnalysisResponse;
import com.obigo.demodong.domain.signal.application.usecase.StockAnalysisUseCase;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.repository.StockRepository;
import com.obigo.demodong.domain.telegram.infrastructure.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BriefingScheduler {

    private final StockRepository stockRepository;
    private final PortfolioDetailRepository portfolioDetailRepository;
    private final StockAnalysisUseCase stockAnalysisUseCase;
    private final TelegramNotifier telegramNotifier;

    @Scheduled(cron = "0 50 8 * * MON-FRI", zone = "Asia/Seoul")
    public void runKoreanMarketBriefing() {
        log.info("모닝브리핑 - 한국 장 전");
        List<Stock> targets = collectAllTargets();
        runBriefing(targets, "한국 장 전 모닝 브리핑");
    }

    @Scheduled(cron = "0 20 22 * * MON-FRI", zone = "Asia/Seoul")
    public void runUsaMarketBriefing() {
        log.info("모닝브리핑 - 미국 장 전");
        List<Stock> targets = collectAllTargets().stream()
                .filter(s -> s.getMarketType() == MarketType.USA)
                .toList();
        runBriefing(targets, "미국 장 전 모닝 브리핑");
    }

    private List<Stock> collectAllTargets() {
        List<Stock> result = new ArrayList<>();

        List<Stock> stocks = stockRepository.findAllByIsWatchlistTrue();
        result.addAll(stocks);

        portfolioDetailRepository.findAllByDeletedFalse().stream()
                .map(PortfolioDetail::getStock)
                .filter(s -> result.stream().noneMatch(r -> r.getId().equals(s.getId())))
                .forEach(result::add);

        return result;
    }

    private void runBriefing(List<Stock> targets, String header) {
        if (targets.isEmpty()) {
            log.info("분석 대상 종목 없음 - 배치 종료");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(header).append("*\n\n");

        for (Stock stock : targets) {
            try {
                StockAnalysisResponse result = stockAnalysisUseCase.execute(stock.getTicker());
                sb.append(formatSignal(result)).append("\n\n");
                log.info("배치 분석 완료 - ticker: {}, signal: {}", stock.getTicker(), result.signalType());
            } catch (Exception e) {
                log.error("배치 분석 실패 - ticker: {}, error: {}", stock.getTicker(), e.getMessage());
                sb.append("⚠️ ").append(stock.getTicker()).append(" 분석 실패\n\n");
            }
        }

        telegramNotifier.sendMessage(sb.toString());
    }

    private String formatSignal(StockAnalysisResponse result) {
        String emoji = switch (result.signalType()) {
            case BUY -> "🟢";
            case SELL -> "🔴";
            case HOLD -> "🟡";
        };
        return emoji + " *" + result.company() + "* - " + result.signalType().name() + "\n" + result.analysis();
    }
}
