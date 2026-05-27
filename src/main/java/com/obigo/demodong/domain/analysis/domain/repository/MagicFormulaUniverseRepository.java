package com.obigo.demodong.domain.analysis.domain.repository;

import com.obigo.demodong.domain.analysis.domain.entity.MagicFormulaUniverse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MagicFormulaUniverseRepository extends JpaRepository<MagicFormulaUniverse, Long> {
    List<MagicFormulaUniverse> findAllByActiveTrue();
    Optional<MagicFormulaUniverse> findByTicker(String ticker);
    boolean existsByTicker(String ticker);
}
