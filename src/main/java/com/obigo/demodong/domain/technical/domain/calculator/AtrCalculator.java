package com.obigo.demodong.domain.technical.domain.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * ATR(평균실제범위) 계산기.
 *
 * 정확한 ATR = max(H-L, |H-PC|, |L-PC|)의 Wilder 14일 평균.
 * PriceSnapshot에 고가/저가가 없어 종가 차이 |close - prevClose|로 근사 계산한다.
 * 향후 고가/저가 데이터 추가 시 완전한 공식으로 교체 가능.
 */
@Component
public class AtrCalculator {

    private static final int PERIOD = 14;
    private static final int SCALE  = 10;

    /**
     * @param closePrices 종가 리스트 (오래된 순서 → 최근 순서). 최소 period+1개 필요.
     * @return ATR14. 데이터 부족 시 null.
     */
    public BigDecimal calculate(List<BigDecimal> closePrices) {
        if (closePrices == null || closePrices.size() < PERIOD + 1) {
            return null;
        }

        // 근사 TR = |close - prevClose|
        List<BigDecimal> trValues = new ArrayList<>();
        for (int i = 1; i < closePrices.size(); i++) {
            trValues.add(closePrices.get(i).subtract(closePrices.get(i - 1)).abs());
        }

        // 초기 ATR = 첫 PERIOD개의 TR 평균
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < PERIOD; i++) {
            sum = sum.add(trValues.get(i));
        }
        BigDecimal atr = sum.divide(BigDecimal.valueOf(PERIOD), SCALE, RoundingMode.HALF_UP);

        // Wilder's Smoothing
        for (int i = PERIOD; i < trValues.size(); i++) {
            atr = atr.multiply(BigDecimal.valueOf(PERIOD - 1))
                    .add(trValues.get(i))
                    .divide(BigDecimal.valueOf(PERIOD), SCALE, RoundingMode.HALF_UP);
        }

        return atr.setScale(2, RoundingMode.HALF_UP);
    }
}
