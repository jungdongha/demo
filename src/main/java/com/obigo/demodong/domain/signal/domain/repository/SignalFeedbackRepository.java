package com.obigo.demodong.domain.signal.domain.repository;

import com.obigo.demodong.domain.signal.domain.entity.SignalFeedback;
import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SignalFeedbackRepository extends JpaRepository<SignalFeedback, Long> {

    Optional<SignalFeedback> findByReportAndDeletedFalse(SignalReport report);

    boolean existsByReportAndDeletedFalse(SignalReport report);

    // Phase 8 — 카테고리별 통계용 (report를 fetch join하여 expectedReasonCategory 접근)
    @Query("SELECT sf FROM SignalFeedback sf JOIN FETCH sf.report WHERE sf.deleted = false")
    List<SignalFeedback> findAllWithReport();

    // 최근 N개 피드백 조회 (최신순)
    @Query("SELECT sf FROM SignalFeedback sf JOIN FETCH sf.report sr WHERE sf.deleted = false ORDER BY sf.createdAt DESC")
    List<SignalFeedback> findRecentWithReport(org.springframework.data.domain.Pageable pageable);
}
