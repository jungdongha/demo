package com.obigo.demodong.domain.notification.application.usecase;

import com.obigo.demodong.domain.analysis.application.dto.response.AnalysisResponse;
import com.obigo.demodong.domain.analysis.application.dto.response.StrategyScoreResponse;
import com.obigo.demodong.domain.analysis.application.usecase.AnalysisUseCase;
import com.obigo.demodong.domain.notification.domain.entity.NotificationLog;
import com.obigo.demodong.domain.notification.domain.service.NotificationLogWriter;
import com.obigo.demodong.domain.notification.domain.service.TelegramNotifier;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 관심종목 일괄 분석 후 텔레그램 발송 유스케이스.
 *
 * <p>처리 순서:</p>
 * <ol>
 *   <li>관심종목 ticker 목록 조회</li>
 *   <li>각 ticker 분석 (캐시 경유)</li>
 *   <li>메시지 포맷 구성</li>
 *   <li>텔레그램 발송</li>
 *   <li>NotificationLog 저장</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchlistNotifyUseCase {

    private final StockReader stockReader;
    private final AnalysisUseCase analysisUseCase;
    private final TelegramNotifier telegramNotifier;
    private final NotificationLogWriter notificationLogWriter;

    public String execute() {
        List<String> tickers = stockReader.findAllByIsWatchlistTrue().stream()
                .map(s -> s.getTicker())
                .toList();

        if (tickers.isEmpty()) {
            log.info("[Notify] 관심종목 없음 — 발송 skip");
            return "관심종목이 없습니다.";
        }

        List<AnalysisResponse> analyses = tickers.stream()
                .map(this::safeAnalyze)
                .filter(r -> r != null)
                .toList();

        String message = buildMessage(analyses);

        try {
            telegramNotifier.send(message);
            notificationLogWriter.save(NotificationLog.success(null, message));
            log.info("[Notify] 텔레그램 발송 성공 — {}종목", analyses.size());
            return "관심종목 %d개 분석 완료. 텔레그램 발송 성공.".formatted(analyses.size());
        } catch (Exception e) {
            log.error("[Notify] 텔레그램 발송 실패: {}", e.getMessage());
            notificationLogWriter.save(NotificationLog.failure(null, message, e.getMessage()));
            return "관심종목 %d개 분석 완료. 텔레그램 발송 실패: %s".formatted(analyses.size(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────

    private AnalysisResponse safeAnalyze(String ticker) {
        try {
            return analysisUseCase.analyze(ticker);
        } catch (Exception e) {
            log.warn("[Notify] 분석 실패 skip — ticker: {}", ticker);
            return null;
        }
    }

    private String buildMessage(List<AnalysisResponse> analyses) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 <b>[데모동 퀀트] 관심종목 일일 리포트</b> (%s)\n\n".formatted(LocalDate.now()));

        for (int i = 0; i < analyses.size(); i++) {
            AnalysisResponse a = analyses.get(i);
            sb.append("%d. <b>%s (%s)</b>\n".formatted(i + 1, a.name(), a.ticker()));

            for (StrategyScoreResponse s : a.strategies()) {
                sb.append("   %s: <b>%d</b>\n".formatted(s.displayName(), s.score()));
            }

            int avg = (int) a.strategies().stream().mapToInt(StrategyScoreResponse::score).average().orElse(0);
            sb.append("   💡 종합: <b>%d점</b>\n\n".formatted(avg));
        }

        sb.append("─────────────────────────\n");
        sb.append("⚠️ 본 정보는 투자 참고용이며 책임은 사용자에게 있습니다.");
        return sb.toString();
    }
}
