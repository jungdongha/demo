package com.obigo.demodong.domain.quant.domain.service;

import com.obigo.demodong.domain.quant.domain.entity.QuantFeatureSnapshot;
import com.obigo.demodong.domain.quant.domain.entity.QuantSignal;
import com.obigo.demodong.domain.quant.domain.repository.QuantFeatureSnapshotRepository;
import com.obigo.demodong.domain.quant.domain.repository.QuantSignalRepository;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuantSignalReader {

    private final QuantSignalRepository quantSignalRepository;
    private final QuantFeatureSnapshotRepository quantFeatureSnapshotRepository;

    public List<QuantSignal> findTodayTop3() {
        return quantSignalRepository.findBySignalDateOrderBySignalRankAsc(LocalDate.now());
    }

    public Optional<QuantSignal> findLatestByTicker(String ticker) {
        return quantSignalRepository.findTopByTickerOrderBySignalDateDescSignalRankAsc(ticker);
    }

    public List<QuantSignal> findLatestSignalsPerTicker() {
        return quantSignalRepository.findLatestSignalsPerTicker();
    }

    public Optional<QuantFeatureSnapshot> findSnapshotBySignalId(Long signalId) {
        return quantFeatureSnapshotRepository.findByQuantSignalId(signalId);
    }

    public boolean existsTodayBatch(MarketType marketType) {
        return quantSignalRepository.existsBySignalDateAndMarketType(LocalDate.now(), marketType);
    }

    public List<QuantSignal> findTodayByMarket(MarketType marketType) {
        return quantSignalRepository.findBySignalDateAndMarketType(LocalDate.now(), marketType);
    }
}
