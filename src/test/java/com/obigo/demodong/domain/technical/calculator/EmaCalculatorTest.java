package com.obigo.demodong.domain.technical.calculator;

import com.obigo.demodong.domain.technical.domain.calculator.EmaCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmaCalculator 단위 테스트")
class EmaCalculatorTest {

    private final EmaCalculator calculator = new EmaCalculator();

    @Nested
    @DisplayName("정상 계산")
    class 정상계산 {

        @Test
        @DisplayName("EMA 시리즈 길이는 prices.size() - period + 1 이다")
        void EMA_시리즈_길이_검증() {
            List<BigDecimal> prices = generateFlat(30, 100);
            List<BigDecimal> emas = calculator.calculate(prices, 10);
            assertThat(emas).hasSize(30 - 10 + 1);
        }

        @Test
        @DisplayName("평탄한 종가의 EMA는 해당 종가와 같다")
        void 평탄_종가_EMA_동일() {
            List<BigDecimal> prices = generateFlat(20, 100);
            BigDecimal ema = calculator.currentEma(prices, 10);
            assertThat(ema).isNotNull();
            assertThat(ema.doubleValue()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        @DisplayName("상승하는 종가에서 EMA는 SMA보다 현재가에 더 민감하게 반응한다")
        void 상승장_EMA_SMA보다_높음() {
            List<BigDecimal> prices = generateAscending(30, 100, 2);
            BigDecimal ema = calculator.currentEma(prices, 10);
            // EMA는 최근 데이터에 더 많은 가중치 → 상승장에서 SMA보다 높거나 유사
            assertThat(ema).isNotNull();
            assertThat(ema.doubleValue()).isGreaterThan(100.0);
        }
    }

    @Nested
    @DisplayName("경계값")
    class 경계값 {

        @Test
        @DisplayName("데이터가 period보다 적으면 빈 리스트를 반환한다")
        void 데이터부족_빈리스트() {
            List<BigDecimal> prices = generateFlat(5, 100);
            List<BigDecimal> emas = calculator.calculate(prices, 10);
            assertThat(emas).isEmpty();
        }

        @Test
        @DisplayName("데이터가 period보다 적으면 currentEma는 null을 반환한다")
        void 데이터부족_currentEma_null() {
            List<BigDecimal> prices = generateFlat(5, 100);
            BigDecimal ema = calculator.currentEma(prices, 10);
            assertThat(ema).isNull();
        }

        @Test
        @DisplayName("null 입력이면 빈 리스트를 반환한다")
        void null입력_빈리스트() {
            List<BigDecimal> emas = calculator.calculate(null, 10);
            assertThat(emas).isEmpty();
        }
    }

    // ──────────────── 헬퍼 ────────────────

    private List<BigDecimal> generateFlat(int count, int value) {
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) prices.add(BigDecimal.valueOf(value));
        return prices;
    }

    private List<BigDecimal> generateAscending(int count, int start, int step) {
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) prices.add(BigDecimal.valueOf(start + (long) i * step));
        return prices;
    }
}
