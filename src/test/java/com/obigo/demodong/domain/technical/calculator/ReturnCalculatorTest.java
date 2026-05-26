package com.obigo.demodong.domain.technical.calculator;

import com.obigo.demodong.domain.technical.domain.calculator.ReturnCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReturnCalculator 단위 테스트")
class ReturnCalculatorTest {

    private final ReturnCalculator returnCalculator = new ReturnCalculator();

    @Test
    @DisplayName("정상 상승 시나리오: 종가가 상승하면 플러스 수익률을 계산한다")
    void 수익률_상승() {
        List<BigDecimal> prices = List.of(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(120)
        );
        BigDecimal result = returnCalculator.calculate(prices, 3);
        // (120 - 100) / 100 * 100 = 20.00
        assertThat(result).isNotNull();
        assertThat(result.compareTo(BigDecimal.valueOf(20.00))).isEqualTo(0);
    }

    @Test
    @DisplayName("정상 하락 시나리오: 종가가 하락하면 마이너스 수익률을 계산한다")
    void 수익률_하락() {
        List<BigDecimal> prices = List.of(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(95),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(80)
        );
        BigDecimal result = returnCalculator.calculate(prices, 3);
        // (80 - 100) / 100 * 100 = -20.00
        assertThat(result).isNotNull();
        assertThat(result.compareTo(BigDecimal.valueOf(-20.00))).isEqualTo(0);
    }

    @Test
    @DisplayName("정상 보합 시나리오: 종가 변화가 없으면 0% 수익률을 계산한다")
    void 수익률_보합() {
        List<BigDecimal> prices = List.of(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100)
        );
        BigDecimal result = returnCalculator.calculate(prices, 2);
        assertThat(result).isNotNull();
        assertThat(result.compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

    @Test
    @DisplayName("데이터 부족 시나리오: 가격 개수가 거래일수+1 미만이면 null을 반환한다")
    void 데이터부족_null() {
        List<BigDecimal> prices = List.of(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(105)
        );
        BigDecimal result = returnCalculator.calculate(prices, 2);
        assertThat(result).isNull();
    }


    @Test
    @DisplayName("경계값 시나리오: 1거래일 수익률을 정확히 계산한다")
    void 일1거래일_수익률() {
        List<BigDecimal> prices = List.of(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(105)
        );
        BigDecimal result = returnCalculator.calculate(prices, 1);
        // (105 - 100) / 100 * 100 = 5.00
        assertThat(result).isNotNull();
        assertThat(result.compareTo(BigDecimal.valueOf(5.00))).isEqualTo(0);
    }

    @Test
    @DisplayName("비정상 데이터 시나리오: 기준 가격이 0원인 경우 null을 반환한다")
    void 기준가0원_null() {
        List<BigDecimal> prices = List.of(
                BigDecimal.ZERO,
                BigDecimal.valueOf(100)
        );
        BigDecimal result = returnCalculator.calculate(prices, 1);
        assertThat(result).isNull();
    }
}
