package com.obigo.demodong.domain.stock.domain.repository;

import com.obigo.demodong.domain.stock.domain.entity.SignalReport;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SignalReportRepository extends JpaRepository<SignalReport, Long> {

    // 종목별 시그널 히스토리 (최신순)
    List<SignalReport> findByStockOrderByCreatedAtDesc(Stock stock);

    // 특정 시간 이후 배치 결과 조회 (오늘 브리핑 목록용)
    List<SignalReport> findBySourceTypeAndCreatedAtAfter(SourceType sourceType, LocalDateTime after);
}
