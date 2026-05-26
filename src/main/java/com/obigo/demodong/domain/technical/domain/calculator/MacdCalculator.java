package com.obigo.demodong.domain.technical.domain.calculator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * MACD(이동평균수렴발산) 계산기.
 * MACD = EMA12 - EMA26
 * Signal = MACD의 EMA9
 * Histogram = MACD - Signal
 */
@Component
@RequiredArgsConstructor
public class MacdCalculator {

    private static final int FAST_PERIOD   = 12;
    private static final int SLOW_PERIOD   = 26;
    private static final int SIGNAL_PERIOD = 9;

    private final EmaCalculator emaCalculator;

    /**
     * @param prices 종가 리스트 (오래된 순서 → 최근 순서). 최소 SLOW + SIGNAL - 1 = 34개 필요.
     * @return MacdResult. 데이터 부족 시 null.
     */
    public MacdResult calculate(List<BigDecimal> prices) {
        if (prices == null || prices.size() < SLOW_PERIOD + SIGNAL_PERIOD - 1) {
            return null;
        }

        List<BigDecimal> ema12Series = emaCalculator.calculate(prices, FAST_PERIOD);
        List<BigDecimal> ema26Series = emaCalculator.calculate(prices, SLOW_PERIOD);

        // ema26의 길이가 ema12보다 짧음 (SLOW-FAST = 14만큼)
        // 두 시리즈를 끝(현재)에서 맞춰 MACD 라인 생성
        int ema26Size = ema26Series.size();
        int ema12Offset = ema12Series.size() - ema26Size;

        List<BigDecimal> macdLine = new ArrayList<>(ema26Size);
        for (int i = 0; i < ema26Size; i++) {
            BigDecimal macd = ema12Series.get(ema12Offset + i).subtract(ema26Series.get(i));
            macdLine.add(macd);
        }

        List<BigDecimal> signalSeries = emaCalculator.calculate(macdLine, SIGNAL_PERIOD);

        BigDecimal currentMacd   = macdLine.get(macdLine.size() - 1);
        BigDecimal currentSignal = signalSeries.isEmpty() ? null : signalSeries.get(signalSeries.size() - 1);
        BigDecimal histogram     = currentSignal != null ? currentMacd.subtract(currentSignal) : null;

        return new MacdResult(currentMacd, currentSignal, histogram);
    }

    public record MacdResult(BigDecimal macd, BigDecimal signal, BigDecimal histogram) {}
}
