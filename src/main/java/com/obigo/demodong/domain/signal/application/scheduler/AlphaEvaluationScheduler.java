package com.obigo.demodong.domain.signal.application.scheduler;

import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.price.domain.port.StockPricePort;
import com.obigo.demodong.domain.price.domain.repository.PriceSnapshotRepository;
import com.obigo.demodong.domain.signal.domain.entity.SignalFeedback;
import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.enums.SignalType;
import com.obigo.demodong.domain.signal.domain.service.SignalFeedbackReader;
import com.obigo.demodong.domain.signal.domain.service.SignalFeedbackWriter;
import com.obigo.demodong.domain.signal.domain.service.SignalReportReader;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Phase 8 — 시그널 Alpha 평가 스케줄러
 * 매 거래일 18:00 (장 마감 후) 실행.
 * T+3 / T+10 / T+20 영업일 이후 주가를 PriceSnapshot에서 조회하여 Alpha를 계산한다.
 *
 * 영업일 근사: 캘린더 일수 기준
 *   T+3  ≈ 5일 경과 후 평가
 *   T+10 ≈ 14일 경과 후 평가
 *   T+20 ≈ 28일 경과 후 평가
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlphaEvaluationScheduler {

    private static final int T3_CALENDAR_DAYS  = 5;
    private static final int T10_CALENDAR_DAYS = 14;
    private static final int T20_CALENDAR_DAYS = 28;

    private final SignalReportReader signalReportReader;
    private final SignalFeedbackReader signalFeedbackReader;
    private final SignalFeedbackWriter signalFeedbackWriter;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final StockPricePort stockPricePort;

    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "Asia/Seoul")
    @Transactional
    public void evaluateAlpha() {
        log.info("[Alpha평가] 스케줄러 시작");
        int evaluated = 0;
        int skipped = 0;

        // ── T+3 평가 ─────────────────────────────────────
        List<SignalReport> t3Targets = signalReportReader.findSignalsWithoutFeedback(T3_CALENDAR_DAYS);
        for (SignalReport report : t3Targets) {
            try {
                refreshPriceIfNeeded(report.getStock());
                LocalDate signalDate = report.getCreatedAt().toLocalDate();
                List<PriceSnapshot> snapshots = fetchSnapshotsAfter(report.getStock(), signalDate);

                BigDecimal priceAtSignal = findPriceAtOrBefore(report.getStock(), signalDate);
                if (priceAtSignal == null || snapshots.size() < 3) {
                    skipped++;
                    continue;
                }

                BigDecimal price3d = snapshots.get(2).getClosePrice();
                boolean wasCorrect3d = isCorrect(report.getSignalType(), priceAtSignal, price3d);

                SignalFeedback feedback = SignalFeedback.builder()
                        .report(report)
                        .priceAtSignal(priceAtSignal)
                        .build();
                feedback.fillT3(price3d, wasCorrect3d);
                signalFeedbackWriter.save(feedback);
                evaluated++;
                log.info("[Alpha평가] T+3 완료 - reportId: {}, ticker: {}, alpha3d: {}%",
                        report.getId(), report.getStock().getTicker(), feedback.getAlpha3d());
            } catch (Exception e) {
                log.warn("[Alpha평가] T+3 실패 - reportId: {}, error: {}", report.getId(), e.getMessage());
                skipped++;
            }
        }

        // ── T+10 평가 ────────────────────────────────────
        List<SignalReport> t10Targets = signalReportReader.findSignalsNeedingT10(T10_CALENDAR_DAYS);
        for (SignalReport report : t10Targets) {
            try {
                refreshPriceIfNeeded(report.getStock());
                LocalDate signalDate = report.getCreatedAt().toLocalDate();
                List<PriceSnapshot> snapshots = fetchSnapshotsAfter(report.getStock(), signalDate);

                if (snapshots.size() < 10) { skipped++; continue; }

                SignalFeedback feedback = signalFeedbackReader.findByReport(report).orElse(null);
                if (feedback == null) { skipped++; continue; }

                BigDecimal price10d = snapshots.get(9).getClosePrice();
                BigDecimal priceMdd  = findMdd(snapshots.subList(0, Math.min(snapshots.size(), 10)));
                boolean wasCorrect10d = isCorrect(report.getSignalType(), feedback.getPriceAtSignal(), price10d);

                feedback.fillT10(price10d, priceMdd, wasCorrect10d);
                evaluated++;
                log.info("[Alpha평가] T+10 완료 - reportId: {}, ticker: {}, alpha10d: {}%, isFailure: {}",
                        report.getId(), report.getStock().getTicker(), feedback.getAlpha10d(), feedback.getIsFailure());
            } catch (Exception e) {
                log.warn("[Alpha평가] T+10 실패 - reportId: {}, error: {}", report.getId(), e.getMessage());
                skipped++;
            }
        }

        // ── T+20 평가 ────────────────────────────────────
        List<SignalReport> t20Targets = signalReportReader.findSignalsNeedingT20(T20_CALENDAR_DAYS);
        for (SignalReport report : t20Targets) {
            try {
                refreshPriceIfNeeded(report.getStock());
                LocalDate signalDate = report.getCreatedAt().toLocalDate();
                List<PriceSnapshot> snapshots = fetchSnapshotsAfter(report.getStock(), signalDate);

                if (snapshots.size() < 20) { skipped++; continue; }

                SignalFeedback feedback = signalFeedbackReader.findByReport(report).orElse(null);
                if (feedback == null) { skipped++; continue; }

                BigDecimal price20d = snapshots.get(19).getClosePrice();
                BigDecimal finalMdd = findMdd(snapshots.subList(0, Math.min(snapshots.size(), 20)));

                feedback.fillT20(price20d, finalMdd);
                evaluated++;
                log.info("[Alpha평가] T+20 완료 - reportId: {}, ticker: {}, alpha20d: {}%",
                        report.getId(), report.getStock().getTicker(), feedback.getAlpha20d());
            } catch (Exception e) {
                log.warn("[Alpha평가] T+20 실패 - reportId: {}, error: {}", report.getId(), e.getMessage());
                skipped++;
            }
        }

        log.info("[Alpha평가] 스케줄러 완료 — 평가: {}건, 스킵: {}건", evaluated, skipped);
    }

    // ─── private helpers ───────────────────────────────────────────────────────

    /**
     * 시그널 날짜 이후의 PriceSnapshot을 날짜 오름차순으로 조회.
     * 최대 30일치만 가져온다.
     */
    private List<PriceSnapshot> fetchSnapshotsAfter(Stock stock, LocalDate signalDate) {
        LocalDate to = signalDate.plusDays(45);
        return priceSnapshotRepository
                .findByStockAndRecordedDateBetweenOrderByRecordedDateAsc(stock, signalDate.plusDays(1), to);
    }

    /**
     * 해당 날짜 또는 그 이전의 가장 가까운 종가를 반환.
     */
    private BigDecimal findPriceAtOrBefore(Stock stock, LocalDate date) {
        Optional<PriceSnapshot> exact = priceSnapshotRepository.findByStockAndRecordedDate(stock, date);
        if (exact.isPresent()) return exact.get().getClosePrice();

        // 직전 5거래일 중 가장 최근 데이터 사용
        List<PriceSnapshot> before = priceSnapshotRepository
                .findByStockAndRecordedDateBetweenOrderByRecordedDateAsc(stock, date.minusDays(7), date.minusDays(1));
        if (before.isEmpty()) return null;
        return before.get(before.size() - 1).getClosePrice();
    }

    /**
     * 해당 스냅샷 목록에서 최저가(MDD 계산용)를 반환.
     */
    private BigDecimal findMdd(List<PriceSnapshot> snapshots) {
        return snapshots.stream()
                .map(PriceSnapshot::getClosePrice)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    /**
     * 시그널 방향에 따라 수익률이 '맞았는지' 판단.
     * BUY → 상승이 '정답', SELL → 하락이 '정답', HOLD → null(판단 불가)
     */
    private boolean isCorrect(SignalType signalType, BigDecimal priceBefore, BigDecimal priceAfter) {
        if (priceBefore == null || priceAfter == null) return false;
        int cmp = priceAfter.compareTo(priceBefore);
        return switch (signalType) {
            case BUY  -> cmp > 0;
            case SELL -> cmp < 0;
            default   -> false;
        };
    }

    /**
     * 최근 30일치 PriceSnapshot이 없으면 KIS API 호출로 갱신.
     */
    private void refreshPriceIfNeeded(Stock stock) {
        try {
            LocalDate monthAgo = LocalDate.now().minusDays(45);
            List<PriceSnapshot> existing = priceSnapshotRepository
                    .findByStockAndRecordedDateBetweenOrderByRecordedDateAsc(stock, monthAgo, LocalDate.now());
            if (existing.size() < 10) {
                stockPricePort.fetchMonthlyPrices(stock);
            }
        } catch (Exception e) {
            log.warn("[Alpha평가] 가격 갱신 실패 - ticker: {}, error: {}", stock.getTicker(), e.getMessage());
        }
    }
}
