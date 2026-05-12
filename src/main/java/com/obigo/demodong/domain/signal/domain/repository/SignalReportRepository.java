package com.obigo.demodong.domain.signal.domain.repository;

import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.enums.SourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SignalReportRepository extends JpaRepository<SignalReport, Long> {

    // 오늘 배치 시그널 조회 (삭제 제외)
    List<SignalReport> findBySourceTypeAndDeletedFalseAndCreatedAtBetweenOrderByCreatedAtDesc(
            SourceType sourceType, LocalDateTime from, LocalDateTime to);

    // ticker 기반 히스토리 조회 (삭제 제외, 페이징)
    Page<SignalReport> findByStock_TickerAndDeletedFalseOrderByCreatedAtDesc(String ticker, Pageable pageable);
}
