package com.obigo.demodong.domain.analysis.domain.service;

import com.obigo.demodong.domain.analysis.domain.enums.MarketRegime;
import com.obigo.demodong.domain.technical.domain.calculator.ReturnCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 시장 국면을 판단하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class MarketRegimeService {

    private final ReturnCalculator returnCalculator;

    /**
     * 지수 종가 리스트를 기반으로 시장 국면을 판단한다.
     *
     * @param indexClosePrices 지수 종가 리스트 (오래된 순서 -> 최근 순서)
     * @return MarketRegime 시장 국면
     */
    public MarketRegime determine(List<BigDecimal> indexClosePrices) {
        if (indexClosePrices == null || indexClosePrices.isEmpty()) {
            return MarketRegime.SIDEWAYS;
        }

        BigDecimal return5d = returnCalculator.calculate(indexClosePrices, 5);
        BigDecimal return20d = returnCalculator.calculate(indexClosePrices, 20);

        // 데이터가 부족하여 수익률 계산이 안 된 경우 기본 횡보장 처리
        if (return5d == null || return20d == null) {
            return MarketRegime.SIDEWAYS;
        }

        // 1. CRISIS: 5일 수익률이 -5% 미만인 경우
        if (return5d.compareTo(BigDecimal.valueOf(-5)) < 0) {
            return MarketRegime.CRISIS;
        }

        // 2. STRONG_BULL: 5일 수익률이 +3% 초과인 경우
        if (return5d.compareTo(BigDecimal.valueOf(3)) > 0) {
            return MarketRegime.STRONG_BULL;
        }

        // 3. BEAR: 5일 수익률이 -1% 미만이거나 20일 수익률이 -2% 미만인 경우
        if (return5d.compareTo(BigDecimal.valueOf(-1)) < 0 || return20d.compareTo(BigDecimal.valueOf(-2)) < 0) {
            return MarketRegime.BEAR;
        }

        // 4. BULL: 5일 수익률 > 0% 이고 20일 수익률 > 0% 인 경우
        if (return5d.compareTo(BigDecimal.ZERO) > 0 && return20d.compareTo(BigDecimal.ZERO) > 0) {
            return MarketRegime.BULL;
        }

        // 5. SIDEWAYS: 그 외의 모든 경우
        return MarketRegime.SIDEWAYS;
    }
}
