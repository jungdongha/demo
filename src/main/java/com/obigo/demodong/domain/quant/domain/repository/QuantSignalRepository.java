package com.obigo.demodong.domain.quant.domain.repository;

import com.obigo.demodong.domain.quant.domain.entity.QuantSignal;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuantSignalRepository extends JpaRepository<QuantSignal, Long> {

    /** 오늘의 TOP3 시그널 (signalRank 오름차순) */
    List<QuantSignal> findBySignalDateOrderBySignalRankAsc(LocalDate signalDate);

    /** 당일 배치가 이미 실행됐는지 확인 */
    boolean existsBySignalDateAndMarketType(LocalDate signalDate, MarketType marketType);

    /** 특정 종목의 최신 시그널 */
    Optional<QuantSignal> findTopByTickerOrderBySignalDateDescSignalRankAsc(String ticker);

    /** 당일 특정 마켓 시그널 전체 삭제 (배치 재실행 시 덮어쓰기용) */
    @Query("SELECT s FROM QuantSignal s WHERE s.signalDate = :date AND s.marketType = :marketType")
    List<QuantSignal> findBySignalDateAndMarketType(@Param("date") LocalDate date,
            @Param("marketType") MarketType marketType);

    /** 전체 유니버스 중 가장 최신 시그널 (스코어 목록용) */
    @Query("SELECT s FROM QuantSignal s WHERE s.signalDate = (SELECT MAX(s2.signalDate) FROM QuantSignal s2 WHERE s2.ticker = s.ticker) AND s.deleted = false")
    List<QuantSignal> findLatestSignalsPerTicker();
}
