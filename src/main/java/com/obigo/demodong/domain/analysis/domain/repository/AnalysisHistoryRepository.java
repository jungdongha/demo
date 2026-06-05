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
}
