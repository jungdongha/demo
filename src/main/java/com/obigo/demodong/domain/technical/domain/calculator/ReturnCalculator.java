package com.obigo.demodong.domain.technical.domain.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 특정 기간 동안의 수익률을 계산하는 계산기.
 * 종가 리스트는 오래된 순서 -> 최근 순서 순으로 정렬되어 있음을 가정한다.
 */
@Component
public class ReturnCalculator {

    private static final int SCALE = 10;

    /**
     * 수익률을 계산한다.
     * 공식: ((최근 종가 - N거래일 전 종가) / N거래일 전 종가) * 100
     *
     * @param prices 종가 리스트 (오래된 순서 -> 최근 순서)
     * @param tradingDays 거래일수 (N)
     * @return 수익률 (%), 데이터 부족이거나 비정상 데이터의 경우 null
     */
    public BigDecimal calculate(List<BigDecimal> prices, int tradingDays) {
        if (prices == null || prices.size() < tradingDays + 1 || tradingDays <= 0) {
            return null;
        }

        BigDecimal latestPrice = prices.get(prices.size() - 1);
        BigDecimal basePrice = prices.get(prices.size() - 1 - tradingDays);

        if (latestPrice == null || basePrice == null || basePrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return latestPrice.subtract(basePrice)
                .divide(basePrice, SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
