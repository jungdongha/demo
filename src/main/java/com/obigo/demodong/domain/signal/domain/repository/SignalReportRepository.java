package com.obigo.demodong.domain.signal.domain.repository;

import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.enums.SourceType;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SignalReportRepository extends JpaRepository<SignalReport, Long> {
    List<SignalReport> findByStockOrderByCreatedAtDesc(Stock stock);
    List<SignalReport> findBySourceTypeAndCreatedAtAfter(SourceType sourceType, LocalDateTime after);
    List<SignalReport> findBySourceTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            SourceType sourceType, LocalDateTime from, LocalDateTime to);
    List<SignalReport> findByStock_IdOrderByCreatedAtDesc(Long stockId);
}
