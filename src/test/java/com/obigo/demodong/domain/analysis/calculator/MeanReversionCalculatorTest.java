package com.obigo.demodong.domain.analysis.calculator;

import com.obigo.demodong.domain.analysis.domain.calculator.MeanReversionCalculator;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.technical.domain.model.TechnicalSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MeanReversionCalculator 단위 테스트")
class MeanReversionCalculatorTest {

    private final MeanReversionCalculator calculator = new MeanReversionCalculator();

    @Nested
    @DisplayName("정상 시나리오")
    class 정상시나리오 {

        @Test
        @DisplayName("과매도 상태(RSI<30, 볼린저 하단, SMA 하회)에서 80점 이상을 반환한다")
        void 과매도_높은점수() {
            TechnicalSnapshot snapshot = buildSnapshot(
                    BigDecimal.valueOf(25),   // RSI 25 → 100점
                    BigDecimal.valueOf(90),    // currentPrice
                    BigDecimal.valueOf(120),   // bollingerUpper
                    BigDecimal.valueOf(80),    // bollingerLower → 하단 근처
                    BigDecimal.valueOf(-8.0)   // deviationFromSma20 (-8%) → 높은점수
            );
            StrategyScore score = calculator.calculate(buildInput(snapshot));
            assertThat(score.score()).isGreaterThanOrEqualTo(80);
            assertThat(score.grade()).isIn("S", "A");
        }

        @Test
        @DisplayName("과매수 상태(RSI>70, 볼린저 상단, SMA 상회)에서 30점 이하를 반환한다")
        void 과매수_낮은점수() {
            TechnicalSnapshot snapshot = buildSnapshot(
                    BigDecimal.valueOf(75),    // RSI 75 → 0점
                    BigDecimal.valueOf(118),   // currentPrice
                    BigDecimal.valueOf(120),   // bollingerUpper → 상단 근처
                    BigDecimal.valueOf(80),    // bollingerLower
                    BigDecimal.valueOf(8.0)    // deviationFromSma20 (+8%) → 낮은점수
            );
            StrategyScore score = calculator.calculate(buildInput(snapshot));
            assertThat(score.score()).isLessThanOrEqualTo(30);
            assertThat(score.grade()).isIn("C", "D");
        }

        @Test
        @DisplayName("중립 상태(RSI=50, 밴드 중간, 이격률 0%)에서 약 50점을 반환한다")
        void 중립상태_50점() {
            TechnicalSnapshot snapshot = buildSnapshot(
                    BigDecimal.valueOf(50),    // RSI 50 → 50점
                    BigDecimal.valueOf(100),   // currentPrice (밴드 중간)
                    BigDecimal.valueOf(120),   // bollingerUpper
                    BigDecimal.valueOf(80),    // bollingerLower
                    BigDecimal.valueOf(0.0)    // deviationFromSma20 (0%) → 50점
            );
            StrategyScore score = calculator.calculate(buildInput(snapshot));
            assertThat(score.score()).isBetween(45, 55);
        }

        @Test
        @DisplayName("점수는 반드시 0~100 범위이다")
        void 점수범위_0에서100() {
            TechnicalSnapshot snapshot = buildSnapshot(
                    BigDecimal.valueOf(10),
                    BigDecimal.valueOf(79),
                    BigDecimal.valueOf(120),
                    BigDecimal.valueOf(80),
                    BigDecimal.valueOf(-15.0)
            );
            StrategyScore score = calculator.calculate(buildInput(snapshot));
            assertThat(score.score()).isBetween(0, 100);
        }
    }

    @Nested
    @DisplayName("null 데이터 처리")
    class null데이터처리 {

        @Test
        @DisplayName("모든 지표가 null이면 50점 중립을 반환한다")
        void 전체null_50점중립() {
            TechnicalSnapshot snapshot = new TechnicalSnapshot(
                    null, null, null, null,
                    null, null, null,
                    Map.of(), null, null, null,
                    null, null,
                    null, null, null, null
            );
            StrategyScore score = calculator.calculate(buildInput(snapshot));
            assertThat(score.score()).isEqualTo(50);
        }
    }

    // ──────────────── 헬퍼 ────────────────

    private TechnicalSnapshot buildSnapshot(BigDecimal rsi, BigDecimal currentPrice,
                                             BigDecimal upper, BigDecimal lower,
                                             BigDecimal deviation) {
        return new TechnicalSnapshot(
                rsi, null, null, null,
                upper, null, lower,
                Map.of(), null, null, null,
                null, null,
                currentPrice, null, null, deviation
        );
    }

    private StrategyInput buildInput(TechnicalSnapshot snapshot) {
        return new StrategyInput("TEST", MarketType.KOR, snapshot);
    }
}
