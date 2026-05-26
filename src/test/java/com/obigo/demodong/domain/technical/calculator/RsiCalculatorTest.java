package com.obigo.demodong.domain.technical.calculator;

import com.obigo.demodong.domain.technical.domain.calculator.RsiCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RsiCalculator 단위 테스트")
class RsiCalculatorTest {

    private final RsiCalculator calculator = new RsiCalculator();

    @Nested
    @DisplayName("정상 계산")
    class 정상계산 {

        @Test
        @DisplayName("14일치 종가로 RSI가 0~100 범위를 반환한다")
        void RSI_정상범위_반환() {
            List<BigDecimal> prices = generateMixedPrices(30);
            BigDecimal rsi = calculator.calculate(prices);
            assertThat(rsi).isNotNull();
            assertThat(rsi.doubleValue()).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("계속 상승하는 종가는 RSI 70 이상을 반환한다")
        void 상승장_RSI_높음() {
            List<BigDecimal> prices = generateAscending(30, 100, 5);
            BigDecimal rsi = calculator.calculate(prices);
            assertThat(rsi.doubleValue()).isGreaterThanOrEqualTo(70.0);
        }

        @Test
        @DisplayName("계속 하락하는 종가는 RSI 30 이하를 반환한다")
        void 하락장_RSI_낮음() {
            List<BigDecimal> prices = generateDescending(30, 300, 5);
            BigDecimal rsi = calculator.calculate(prices);
            assertThat(rsi.doubleValue()).isLessThanOrEqualTo(30.0);
        }
    }

    @Nested
    @DisplayName("경계값")
    class 경계값 {

        @Test
        @DisplayName("데이터 부족(14개 이하)이면 null을 반환한다")
        void 데이터부족_null반환() {
            List<BigDecimal> prices = List.of(
                    BigDecimal.valueOf(100), BigDecimal.valueOf(105)
            );
            assertThat(calculator.calculate(prices)).isNull();
        }

        @Test
        @DisplayName("모든 종가가 동일하면 RSI 50을 반환한다")
        void 변동없음_중립50() {
            List<BigDecimal> prices = Collections.nCopies(20, BigDecimal.valueOf(100));
            BigDecimal rsi = calculator.calculate(prices);
            assertThat(rsi).isNotNull();
            assertThat(rsi.doubleValue()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("손실이 전혀 없으면 RSI 100을 반환한다")
        void 손실없음_RSI100() {
            List<BigDecimal> prices = generateAscending(20, 100, 1);
            BigDecimal rsi = calculator.calculate(prices);
            assertThat(rsi.doubleValue()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("이익이 전혀 없으면 RSI 0을 반환한다")
        void 이익없음_RSI0() {
            List<BigDecimal> prices = generateDescending(20, 200, 1);
            BigDecimal rsi = calculator.calculate(prices);
            assertThat(rsi.doubleValue()).isEqualTo(0.0);
        }
    }

    // ──────────────── 헬퍼 ────────────────

    private List<BigDecimal> generateAscending(int count, int start, int step) {
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) prices.add(BigDecimal.valueOf(start + (long) i * step));
        return prices;
    }

    private List<BigDecimal> generateDescending(int count, int start, int step) {
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) prices.add(BigDecimal.valueOf(start - (long) i * step));
        return prices;
    }

    private List<BigDecimal> generateMixedPrices(int count) {
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            prices.add(BigDecimal.valueOf(100 + (i % 5 == 0 ? -3 : 2)));
        }
        return prices;
    }
}
