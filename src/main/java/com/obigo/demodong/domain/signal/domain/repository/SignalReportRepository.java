package com.obigo.demodong.domain.signal.domain.repository;

import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.enums.SourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SignalReportRepository extends JpaRepository<SignalReport, Long> {

       // 오늘 배치 시그널 조회 (삭제 제외)
       List<SignalReport> findBySourceTypeAndDeletedFalseAndCreatedAtBetweenOrderByCreatedAtDesc(
                     SourceType sourceType, LocalDateTime from, LocalDateTime to);

       // ticker 기반 히스토리 조회 (삭제 제외, 페이징)
       Page<SignalReport> findByStock_TickerAndDeletedFalseOrderByCreatedAtDesc(String ticker, Pageable pageable);

       // Phase 8 — 피드백 없는 시그널 조회 (T+3 평가 대상: 5일+ 경과)
       @Query("SELECT sr FROM SignalReport sr WHERE sr.deleted = false AND sr.createdAt < :cutoff " +
                     "AND NOT EXISTS (SELECT sf FROM SignalFeedback sf WHERE sf.report = sr AND sf.deleted = false)")
       List<SignalReport> findSignalsWithoutFeedback(@Param("cutoff") LocalDateTime cutoff);

       // Phase 8 — T+10 미평가 시그널 조회 (14일+ 경과)
       @Query("SELECT sr FROM SignalReport sr JOIN SignalFeedback sf ON sf.report = sr " +
                     "WHERE sr.deleted = false AND sf.deleted = false AND sf.priceAfter10d IS NULL AND sr.createdAt < :cutoff")
       List<SignalReport> findSignalsNeedingT10(@Param("cutoff") LocalDateTime cutoff);

       // Phase 8 — T+20 미평가 시그널 조회 (28일+ 경과)
       @Query("SELECT sr FROM SignalReport sr JOIN SignalFeedback sf ON sf.report = sr " +
                     "WHERE sr.deleted = false AND sf.deleted = false AND sf.priceAfter20d IS NULL AND sr.createdAt < :cutoff")
       List<SignalReport> findSignalsNeedingT20(@Param("cutoff") LocalDateTime cutoff);
}
