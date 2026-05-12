package com.obigo.demodong.domain.signal.application.scheduler;

import com.obigo.demodong.domain.portfolio.domain.service.PortfolioReader;
import com.obigo.demodong.domain.signal.application.dto.response.StockAnalysisResponse;
import com.obigo.demodong.domain.signal.application.usecase.StockAnalysisUseCase;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import com.obigo.demodong.domain.telegram.infrastructure.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class BriefingScheduler {

    private final StockReader stockReader;
    private final PortfolioReader portfolioReader;
    private final StockAnalysisUseCase stockAnalysisUseCase;
    private final TelegramNotifier telegramNotifier;

    @Scheduled(cron = "0 50 8 * * MON-FRI", zone = "Asia/Seoul")
    public void runKoreanMarketBriefing() {
        log.info("모닝브리핑 - 한국 장 전");
        List<Stock> targets = collectAllTargets();
        runBriefing(targets, "📊 관심 종목 시그널");
    }

    @Scheduled(cron = "0 20 22 * * MON-FRI", zone = "Asia/Seoul")
    public void runUsaMarketBriefing() {
        log.info("모닝브리핑 - 미국 장 전");
        List<Stock> targets = collectAllTargets().stream()
                .filter(s -> s.getMarketType() == MarketType.USA)
                .toList();
        runBriefing(targets, "📊 미국 관심 종목 시그널");
    }

    private List<Stock> collectAllTargets() {
        List<Stock> result = new ArrayList<>();

        result.addAll(stockReader.findAllByIsWatchlistTrue());

        portfolioReader.findAll().stream()
                .map(detail -> detail.getStock())
                .filter(s -> result.stream().noneMatch(r -> r.getId().equals(s.getId())))
                .forEach(result::add);

        return result;
    }

    private void runBriefing(List<Stock> targets, String sectionTitle) {
        if (targets.isEmpty()) {
            log.info("분석 대상 종목 없음 - 배치 종료");
            return;
        }

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd (E)", Locale.KOREAN));
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        StringBuilder sb = new StringBuilder();
        sb.append("[Jurine 모닝 브리핑] ").append(dateStr).append(" ").append(timeStr).append("\n\n");
        sb.append("━━━━━━ ").append(sectionTitle).append(" ━━━━━━\n\n");

        for (Stock stock : targets) {
            try {
                StockAnalysisResponse result = stockAnalysisUseCase.executeScheduled(stock.getTicker());
                sb.append(formatSignal(result)).append("\n\n");
                log.info("배치 분석 완료 - ticker: {}, signal: {}", stock.getTicker(), result.signalType());
            } catch (Exception e) {
                log.error("배치 분석 실패 - ticker: {}, error: {}", stock.getTicker(), e.getMessage());
                sb.append("⚠️ ").append(stock.getTicker()).append(" 분석 실패\n\n");
            }
        }

        sb.append("─────────────────────────\n");
        sb.append("⚠️ 본 시그널은 AI 참고 정보입니다. 투자 판단과 책임은 전적으로 사용자에게 있습니다.");

        telegramNotifier.sendMessage(sb.toString());
    }

    private String formatSignal(StockAnalysisResponse result) {
        String emoji = switch (result.signalType()) {
            case BUY -> "🟢";
            case SELL -> "🔴";
            case HOLD -> "🟡";
        };
        String reasonLines = buildNumberedReason(result.reason(), result.analysis());
        return emoji + " <b>" + result.company() + "</b> — " + result.signalType().name() + "\n" + reasonLines;
    }

    private String buildNumberedReason(String reason, String analysis) {
        String[] nums = {"①", "②", "③"};
        String source = (reason != null && !reason.isBlank()) ? reason : analysis;
        String[] lines = source.split("\n");

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            String trimmed = line.replaceFirst("^-\\s*", "").trim();
            if (!trimmed.isEmpty() && count < nums.length) {
                sb.append(nums[count++]).append(" ").append(trimmed).append("\n");
            }
        }
        if (sb.isEmpty()) {
            String fallback = analysis.length() > 100 ? analysis.substring(0, 100) + "..." : analysis;
            return fallback;
        }
        return sb.toString().trim();
    }
}
