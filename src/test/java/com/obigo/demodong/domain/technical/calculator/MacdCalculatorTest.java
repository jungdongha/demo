package com.obigo.demodong.domain.technical.calculator;

import com.obigo.demodong.domain.technical.domain.calculator.EmaCalculator;
import com.obigo.demodong.domain.technical.domain.calculator.MacdCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MacdCalculator 단위 테스트")
class MacdCalculatorTest {

    private final MacdCalculator calculator = new MacdCalculator(new EmaCalculator());

    @Nested
    @DisplayName("정상 계산")
    class 정상계산 {

        @Test
        @DisplayName("충분한 데이터로 MACD, Signal, Histogram을 모두 반환한다")
        void MACD_3값_모두반환() {
            List<BigDecimal> prices = generateAscending(60, 100, 1);
            MacdCalculator.MacdResult result = calculator.calculate(prices);
            assertThat(result).isNotNull();
            assertThat(result.macd()).isNotNull();
            assertThat(result.signal()).isNotNull();
            assertThat(result.histogram()).isNotNull();
        }

        @Test
        @DisplayName("Histogram = MACD - Signal 이다")
        void Histogram_MACD_빼기_Signal() {
            List<BigDecimal> prices = generateMixed(60);
            MacdCalculator.MacdResult result = calculator.calculate(prices);
            assertThat(result).isNotNull();
            BigDecimal expected = result.macd().subtract(result.signal());
            assertThat(result.histogram()).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("상승장에서 MACD는 양수이다")
        void 상승장_MACD_양수() {
            List<BigDecimal> prices = generateAscending(60, 100, 3);
            MacdCalculator.MacdResult result = calculator.calculate(prices);
            assertThat(result).isNotNull();
            assertThat(result.macd().doubleValue()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("하락장에서 MACD는 음수이다")
        void 하락장_MACD_음수() {
            List<BigDecimal> prices = generateDescending(60, 500, 3);
            MacdCalculator.MacdResult result = calculator.calculate(prices);
            assertThat(result).isNotNull();
            assertThat(result.macd().doubleValue()).isLessThan(0.0);
        }
    }

    @Nested
    @DisplayName("경계값")
    class 경계값 {

        @Test
        @DisplayName("데이터 부족(33개 이하)이면 null을 반환한다")
        void 데이터부족_null반환() {
            List<BigDecimal> prices = generateAscending(30, 100, 1);
            assertThat(calculator.calculate(prices)).isNull();
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

    private List<BigDecimal> generateMixed(int count) {
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            prices.add(BigDecimal.valueOf(100 + (i % 7 < 4 ? i : -i % 10)));
        }
        return prices;
    }
}
