package com.obigo.demodong.domain.signal.domain.service;

import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.enums.SourceType;
import com.obigo.demodong.domain.signal.domain.repository.SignalReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignalReportReader {

    private final SignalReportRepository signalReportRepository;

    public List<SignalReport> findTodayScheduled() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return signalReportRepository
                .findBySourceTypeAndDeletedFalseAndCreatedAtBetweenOrderByCreatedAtDesc(SourceType.SCHEDULED, start, end);
    }

    public Page<SignalReport> findByTicker(String ticker, Pageable pageable) {
        return signalReportRepository.findByStock_TickerAndDeletedFalseOrderByCreatedAtDesc(ticker, pageable);
    }

    // Phase 8 — 피드백 없는 시그널 (T+3 평가 대상)
    public List<SignalReport> findSignalsWithoutFeedback(int calendarDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(calendarDays);
        return signalReportRepository.findSignalsWithoutFeedback(cutoff);
    }

    // Phase 8 — T+10 미평가 시그널
    public List<SignalReport> findSignalsNeedingT10(int calendarDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(calendarDays);
        return signalReportRepository.findSignalsNeedingT10(cutoff);
    }

    // Phase 8 — T+20 미평가 시그널
    public List<SignalReport> findSignalsNeedingT20(int calendarDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(calendarDays);
        return signalReportRepository.findSignalsNeedingT20(cutoff);
    }
}
