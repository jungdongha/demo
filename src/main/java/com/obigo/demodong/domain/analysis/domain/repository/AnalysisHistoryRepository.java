package com.obigo.demodong.domain.analysis.domain.repository;

import com.obigo.demodong.domain.analysis.domain.entity.AnalysisHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistory, Long> {

    Optional<AnalysisHistory> findByTickerAndAnalyzeDate(String ticker, LocalDate analyzeDate);

    List<AnalysisHistory> findByTickerAndAnalyzeDateAfterOrderByAnalyzeDateAsc(
            String ticker, LocalDate from);

    @Modifying
    @Query("DELETE FROM AnalysisHistory h WHERE h.analyzeDate < :cutoff")
    void deleteByAnalyzeDateBefore(@Param("cutoff") LocalDate cutoff);

    /**
     * 모든 ticker의 가장 최신 analyze_date 레코드 조회 (랭킹용).
     * ticker별 최신 1건씩 반환.
     */
    @Query("SELECT h FROM AnalysisHistory h WHERE h.analyzeDate = " +
           "(SELECT MAX(h2.analyzeDate) FROM AnalysisHistory h2 WHERE h2.ticker = h.ticker)")
    List<AnalysisHistory> findLatestPerTicker();
}
