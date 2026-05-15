package com.obigo.demodong.domain.quant.application.usecase;

import com.obigo.demodong.domain.quant.application.dto.response.*;
import com.obigo.demodong.domain.quant.application.exception.QuantErrorCode;
import com.obigo.demodong.domain.quant.domain.entity.QuantFeatureSnapshot;
import com.obigo.demodong.domain.quant.domain.entity.QuantSignal;
import com.obigo.demodong.domain.quant.domain.entity.QuantUniverse;
import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import com.obigo.demodong.domain.quant.domain.port.QuantDataPort;
import com.obigo.demodong.domain.quant.domain.service.MarketRegimeService;
import com.obigo.demodong.domain.quant.domain.service.QuantSignalReader;
import com.obigo.demodong.domain.quant.domain.service.QuantUniverseReader;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.global.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quant 조회 Facade — 모든 조회 API의 진입점.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuantSignalUseCase {

    private final QuantSignalReader quantSignalReader;
    private final QuantUniverseReader quantUniverseReader;
    private final MarketRegimeService marketRegimeService;
    private final QuantDataPort quantDataPort;

    /** 오늘의 TOP3 Quant 시그널 조회 (QuantFeatureSnapshot 포함) */
    public List<QuantSignalResponse> getTopSignals() {
        List<QuantSignal> signals = quantSignalReader.findTodayTop3();
        return signals.stream()
                .map(signal -> {
                    QuantFeatureSnapshot snapshot = quantSignalReader
                            .findSnapshotBySignalId(signal.getId())
                            .orElse(null);
                    return QuantSignalResponse.from(signal, snapshot);
                })
                .toList();
    }

    /** 전체 유니버스 최신 Quant Score 목록 */
    public List<QuantScoreResponse> getScores() {
        return quantSignalReader.findLatestSignalsPerTicker().stream()
                .map(QuantScoreResponse::from)
                .toList();
    }

    /** 특정 종목 Quant Score 상세 (QuantFeatureSnapshot 포함) */
    public QuantScoreDetailResponse getScoreDetail(String ticker) {
        QuantSignal signal = quantSignalReader.findLatestByTicker(ticker)
                .orElseThrow(() -> new ApplicationException(QuantErrorCode.QUANT_SIGNAL_NOT_FOUND));

        QuantFeatureSnapshot snapshot = quantSignalReader
                .findSnapshotBySignalId(signal.getId())
                .orElse(null);

        return QuantScoreDetailResponse.from(signal, snapshot);
    }

    /** 현재 분석 유니버스 목록 (active=true) */
    public List<QuantUniverseResponse> getUniverse() {
        List<QuantUniverse> universe = quantUniverseReader.findAllActive();
        if (universe.isEmpty()) {
            throw new ApplicationException(QuantErrorCode.QUANT_UNIVERSE_EMPTY);
        }
        return universe.stream()
                .map(QuantUniverseResponse::from)
                .toList();
    }

    /** 현재 시장 국면 조회 (KOR/USA) */
    public MarketRegimeResponse getMarketRegime() {
        double korReturn = quantDataPort.fetchIndexReturn5d(MarketType.KOR);
        double usaReturn = quantDataPort.fetchIndexReturn5d(MarketType.USA);

        MarketRegime korRegime = marketRegimeService.analyze(korReturn);
        MarketRegime usaRegime = marketRegimeService.analyze(usaReturn);

        return MarketRegimeResponse.of(korRegime, korReturn, usaRegime);
    }
}
