package com.obigo.demodong.domain.technical.calculator;

import com.obigo.demodong.domain.technical.domain.calculator.BollingerBandCalculator;
import com.obigo.demodong.domain.technical.domain.calculator.SmaCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BollingerBandCalculator 단위 테스트")
class BollingerBandCalculatorTest {

    private final BollingerBandCalculator calculator = new BollingerBandCalculator(new SmaCalculator());

    @Nested
    @DisplayName("정상 계산")
    class 정상계산 {

        @Test
        @DisplayName("Upper > Mid > Lower 순서를 보장한다")
        void 밴드_순서_Upper_Mid_Lower() {
            List<BigDecimal> prices = generateMixed(30);
            BollingerBandCalculator.BollingerResult result = calculator.calculate(prices);
            assertThat(result).isNotNull();
            assertThat(result.upper().doubleValue()).isGreaterThan(result.mid().doubleValue());
            assertThat(result.mid().doubleValue()).isGreaterThan(result.lower().doubleValue());
        }

        @Test
        @DisplayName("Mid는 최근 20일 SMA와 같다")
        void Mid는_SMA20() {
            List<BigDecimal> prices = generateAscending(25, 100, 1);
            BollingerBandCalculator.BollingerResult result = calculator.calculate(prices);
            assertThat(result).isNotNull();

            // 최근 20일 SMA 직접 계산
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 5; i < 25; i++) sum = sum.add(prices.get(i));
            BigDecimal expectedMid = sum.divide(BigDecimal.valueOf(20), 2, java.math.RoundingMode.HALF_UP);

            assertThat(result.mid()).isEqualByComparingTo(expectedMid);
        }

        @Test
        @DisplayName("변동성이 없으면(모든 종가 동일) Upper == Mid == Lower이다")
        void 변동없음_밴드_동일() {
            List<BigDecimal> prices = Collections.nCopies(25, BigDecimal.valueOf(100));
            BollingerBandCalculator.BollingerResult result = calculator.calculate(prices);
            assertThat(result).isNotNull();
            assertThat(result.upper()).isEqualByComparingTo(result.mid());
            assertThat(result.lower()).isEqualByComparingTo(result.mid());
        }
    }

    @Nested
    @DisplayName("경계값")
    class 경계값 {

        @Test
        @DisplayName("데이터 20개 미만이면 null을 반환한다")
        void 데이터부족_null반환() {
            List<BigDecimal> prices = generateAscending(15, 100, 1);
            assertThat(calculator.calculate(prices)).isNull();
        }

        @Test
        @DisplayName("데이터 정확히 20개면 결과를 반환한다")
        void 데이터_정확히20개_정상반환() {
            List<BigDecimal> prices = generateAscending(20, 100, 1);
            assertThat(calculator.calculate(prices)).isNotNull();
        }
    }

    // ──────────────── 헬퍼 ────────────────

    private List<BigDecimal> generateAscending(int count, int start, int step) {
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) prices.add(BigDecimal.valueOf(start + (long) i * step));
        return prices;
    }

    private List<BigDecimal> generateMixed(int count) {
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double v = 100 + 10 * Math.sin(i * 0.5);
            prices.add(BigDecimal.valueOf(Math.round(v)));
        }
        return prices;
    }
}
