package com.obigo.demodong.domain.analysis.domain.repository;

import com.obigo.demodong.domain.analysis.domain.entity.MagicFormulaRank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MagicFormulaRankRepository extends JpaRepository<MagicFormulaRank, Long> {
    Optional<MagicFormulaRank> findByTickerAndRankDate(String ticker, LocalDate rankDate);
    Optional<MagicFormulaRank> findTopByTickerOrderByRankDateDesc(String ticker);
    List<MagicFormulaRank> findAllByRankDateOrderByCombinedRankAsc(LocalDate rankDate);
    boolean existsByTickerAndRankDate(String ticker, LocalDate rankDate);
}
