package com.obigo.demodong.domain.quant.domain.calculator;

import com.obigo.demodong.domain.quant.domain.model.DailyQuote;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureResult;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SectorRelativeStrengthCalculator 단위 테스트")
class SectorRelativeStrengthCalculatorTest {

    private SectorRelativeStrengthCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SectorRelativeStrengthCalculator();
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("섹터 평균보다 +10%p 높으면 100점 반환")
        void 섹터_상대강도_최대이면_100점이다() {
            // selfMomentum = +5%, sectorAvg = -5% → relative = +10% → 100점
            List<DailyQuote> prices = List.of(
                    new DailyQuote(LocalDate.now(),              BigDecimal.valueOf(52500), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(4), BigDecimal.valueOf(50000), 1_000_000L)
            );
            QuantFeatureInput input = buildInput(prices, -5.0);
            QuantFeatureResult result = calculator.calculate(input);

            assertThat(result.normalizedScore()).isEqualTo(100.0);
            assertThat(result.rawValue()).isCloseTo(10.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("섹터 평균보다 -10%p 낮으면 0점 반환")
        void 섹터_상대강도_최소이면_0점이다() {
            // selfMomentum = -5%, sectorAvg = +5% → relative = -10% → 0점
            List<DailyQuote> prices = List.of(
                    new DailyQuote(LocalDate.now(),              BigDecimal.valueOf(47500), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(4), BigDecimal.valueOf(50000), 1_000_000L)
            );
            QuantFeatureInput input = buildInput(prices, 5.0);
            QuantFeatureResult result = calculator.calculate(input);

            assertThat(result.normalizedScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("섹터 평균과 동일하면 50점 반환 (중립)")
        void 섹터_평균과_동일하면_50점이다() {
            // selfMomentum = +3%, sectorAvg = +3% → relative = 0% → 50점
            List<DailyQuote> prices = List.of(
                    new DailyQuote(LocalDate.now(),              BigDecimal.valueOf(51500), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(4), BigDecimal.valueOf(50000), 1_000_000L)
            );
            QuantFeatureInput input = buildInput(prices, 3.0);
            QuantFeatureResult result = calculator.calculate(input);

            assertThat(result.normalizedScore()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(1.0));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("가격 데이터가 부족하면 50점 중립 반환")
        void 가격데이터_부족이면_중립이다() {
            List<DailyQuote> prices = List.of();
            QuantFeatureInput input = buildInput(prices, 0.0);
            QuantFeatureResult result = calculator.calculate(input);

            assertThat(result.normalizedScore()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("섹터 내 종목 1개(sectorAvg=selfMomentum)이면 50점 중립 반환")
        void 섹터_종목_1개이면_중립이다() {
            // selfMomentum = +3%, sectorAvg = +3% (자기 자신만 있는 섹터)
            List<DailyQuote> prices = List.of(
                    new DailyQuote(LocalDate.now(),              BigDecimal.valueOf(51500), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(4), BigDecimal.valueOf(50000), 1_000_000L)
            );
            double selfMomentum = 3.0;
            QuantFeatureInput input = buildInput(prices, selfMomentum); // sectorAvg == selfMomentum
            QuantFeatureResult result = calculator.calculate(input);

            assertThat(result.normalizedScore()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(1.0));
        }

        @Test
        @DisplayName("점수는 항상 0~100 사이이다")
        void 점수는_0에서_100_사이이다() {
            List<DailyQuote> prices = List.of(
                    new DailyQuote(LocalDate.now(),              BigDecimal.valueOf(100000), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(4), BigDecimal.valueOf(50000),  1_000_000L)
            );
            QuantFeatureInput input = buildInput(prices, -20.0); // 극단적 사례
            QuantFeatureResult result = calculator.calculate(input);

            assertThat(result.normalizedScore()).isBetween(0.0, 100.0);
        }
    }

    private QuantFeatureInput buildInput(List<DailyQuote> prices, double sectorAvg) {
        return new QuantFeatureInput("005930", "삼성전자", MarketType.KOR,
                prices, List.of(), 0.0, null, sectorAvg);
    }
}
