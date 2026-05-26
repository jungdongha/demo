package com.obigo.demodong.domain.technical.application.usecase;

import com.obigo.demodong.domain.technical.domain.calculator.*;
import com.obigo.demodong.domain.technical.domain.model.TechnicalSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 기술적 분석 유스케이스.
 * 종가 리스트를 받아 모든 기술적 지표를 계산하고 TechnicalSnapshot을 반환한다.
 *
 * <p>입력 데이터는 호출자(AnalysisUseCase, Phase 7)가 StockPricePort로 조회해 전달한다.
 * 이 UseCase는 순수 계산 레이어로 외부 API에 의존하지 않는다.</p>
 *
 * <p>최소 권장 데이터: 200일치 (EMA200 계산용). 부족 시 해당 필드는 null 반환.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechnicalAnalysisUseCase {

    private static final int[] EMA_PERIODS = {20, 50, 150, 200};
    private static final int EMA200_LOOKBACK = 20; // Minervini 조건2: 20거래일 전 EMA200

    private final RsiCalculator             rsiCalculator;
    private final MacdCalculator            macdCalculator;
    private final BollingerBandCalculator   bollingerBandCalculator;
    private final EmaCalculator             emaCalculator;
    private final SmaCalculator             smaCalculator;
    private final AtrCalculator             atrCalculator;
    private final AdxCalculator             adxCalculator;

    /**
     * 기술적 분석 스냅샷을 계산한다.
     *
     * @param closePrices 종가 리스트 (오래된 순서 → 최근 순서)
     * @param high52w     52주 최고가 (없으면 null)
     * @param low52w      52주 최저가 (없으면 null)
     * @return TechnicalSnapshot. 데이터 부족 필드는 null.
     */
    public TechnicalSnapshot analyze(List<BigDecimal> closePrices,
                                     BigDecimal high52w,
                                     BigDecimal low52w) {
        if (closePrices == null || closePrices.isEmpty()) {
            return emptySnapshot();
        }

        BigDecimal currentPrice = closePrices.get(closePrices.size() - 1);

        // RSI
        BigDecimal rsi14 = rsiCalculator.calculate(closePrices);

        // MACD
        MacdCalculator.MacdResult macdResult = macdCalculator.calculate(closePrices);

        // 볼린저 밴드
        BollingerBandCalculator.BollingerResult bollingerResult = bollingerBandCalculator.calculate(closePrices);

        // EMA 현재값 (20/50/150/200)
        Map<Integer, BigDecimal> emaMap = new HashMap<>();
        for (int period : EMA_PERIODS) {
            emaMap.put(period, emaCalculator.currentEma(closePrices, period));
        }

        // EMA200 20거래일 전 값 (Minervini 조건2 — 우상향 판단)
        BigDecimal ema200OneMonthAgo = computeEma200OneMonthAgo(closePrices);

        // SMA
        BigDecimal sma20 = smaCalculator.calculate(closePrices, 20);
        BigDecimal sma60 = smaCalculator.calculate(closePrices, 60);

        // 이격률 = (현재가 - SMA20) / SMA20 × 100
        BigDecimal deviationFromSma20 = computeDeviation(currentPrice, sma20);

        // ATR, ADX
        BigDecimal atr14 = atrCalculator.calculate(closePrices);
        BigDecimal adx14 = adxCalculator.calculate(closePrices);

        return new TechnicalSnapshot(
                rsi14,
                macdResult != null ? macdResult.macd()      : null,
                macdResult != null ? macdResult.signal()    : null,
                macdResult != null ? macdResult.histogram() : null,
                bollingerResult != null ? bollingerResult.upper() : null,
                bollingerResult != null ? bollingerResult.mid()   : null,
                bollingerResult != null ? bollingerResult.lower() : null,
                Collections.unmodifiableMap(emaMap),
                ema200OneMonthAgo,
                atr14,
                adx14,
                sma20,
                sma60,
                currentPrice,
                high52w,
                low52w,
                deviationFromSma20
        );
    }

    private BigDecimal computeEma200OneMonthAgo(List<BigDecimal> closePrices) {
        int minSize = 200 + EMA200_LOOKBACK;
        if (closePrices.size() < minSize) {
            return null;
        }
        List<BigDecimal> pricesBefore = closePrices.subList(0, closePrices.size() - EMA200_LOOKBACK);
        return emaCalculator.currentEma(pricesBefore, 200);
    }

    private BigDecimal computeDeviation(BigDecimal currentPrice, BigDecimal sma20) {
        if (currentPrice == null || sma20 == null || sma20.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        // (현재가 - SMA20) / SMA20 × 100
        return currentPrice.subtract(sma20)
                .divide(sma20, 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private TechnicalSnapshot emptySnapshot() {
        return new TechnicalSnapshot(
                null, null, null, null,
                null, null, null,
                Map.of(),
                null, null, null,
                null, null,
                null, null, null,
                null
        );
    }
}
