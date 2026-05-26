package com.obigo.demodong.domain.technical.domain.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * ADX(평균방향지수) 계산기.
 *
 * 정확한 ADX 계산은 고가/저가(+DI, -DI)가 필요하다.
 * PriceSnapshot에 고가/저가가 없어 현재 Phase에서는 null을 반환한다.
 * 전략 Calculator는 null → 50점 중립으로 처리한다.
 * 향후 PriceSnapshot에 고가/저가 추가 시 완전한 공식으로 교체 가능.
 */
@Component
public class AdxCalculator {

    /**
     * @return 항상 null (고가/저가 데이터 미지원)
     */
    public BigDecimal calculate(List<BigDecimal> closePrices) {
        return null;
    }
}
