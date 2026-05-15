package com.obigo.demodong.domain.quant.domain.service;

import com.obigo.demodong.domain.quant.domain.entity.QuantUniverse;
import com.obigo.demodong.domain.quant.domain.repository.QuantUniverseRepository;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuantUniverseReader {

    private final QuantUniverseRepository quantUniverseRepository;

    public List<QuantUniverse> findAllActive() {
        return quantUniverseRepository.findByActiveTrue();
    }

    public List<QuantUniverse> findActiveByMarket(MarketType marketType) {
        return quantUniverseRepository.findByMarketTypeAndActiveTrue(marketType);
    }

    public boolean isUniverseEmpty() {
        return quantUniverseRepository.countByActiveTrue() == 0;
    }

    public boolean existsByTickerAndMarket(String ticker, MarketType marketType) {
        return quantUniverseRepository.existsByTickerAndMarketType(ticker, marketType);
    }
}
