package com.obigo.demodong.domain.quant.domain.repository;

import com.obigo.demodong.domain.quant.domain.entity.QuantUniverse;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuantUniverseRepository extends JpaRepository<QuantUniverse, Long> {

    List<QuantUniverse> findByActiveTrue();

    List<QuantUniverse> findByMarketTypeAndActiveTrue(MarketType marketType);

    boolean existsByTickerAndMarketType(String ticker, MarketType marketType);

    long countByActiveTrue();
}
