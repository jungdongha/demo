package com.obigo.demodong.domain.quant.domain.calculator;

import com.obigo.demodong.domain.quant.domain.model.DailyQuote;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureResult;
import com.obigo.demodong.domain.quant.domain.model.QuantFundamentalData;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValuationScoreCalculator 단위 테스트")
class ValuationScoreCalculatorTest {

    private ValuationScoreCalculator calculator;
    private List<DailyQuote> dummyPrices;

    @BeforeEach
    void setUp() {
        calculator = new ValuationScoreCalculator();
        dummyPrices = List.of(new DailyQuote(LocalDate.now(), BigDecimal.valueOf(50000), 1_000_000L));
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("PER=8, PBR=0.9이면 만점에 가까운 점수를 반환한다")
        void 저평가_종목은_만점에_가깝다() {
            QuantFundamentalData f = new QuantFundamentalData(
                    BigDecimal.valueOf(8.0), BigDecimal.valueOf(0.9), null, BigDecimal.valueOf(50000));
            QuantFeatureResult result = calculator.calculate(buildInput(f));

            assertThat(result.normalizedScore()).isGreaterThan(90.0);
        }

        @Test
        @DisplayName("PER=20, PBR=2이면 50점을 반환한다 (중간값)")
        void 중간_밸류에이션은_50점이다() {
            QuantFundamentalData f = new QuantFundamentalData(
                    BigDecimal.valueOf(20.0), BigDecimal.valueOf(2.0), null, BigDecimal.valueOf(50000));
            QuantFeatureResult result = calculator.calculate(buildInput(f));

            assertThat(result.normalizedScore()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(1.0));
        }

        @Test
        @DisplayName("PER=35, PBR=4이면 0점에 가까운 점수를 반환한다 (고평가)")
        void 고평가_종목은_0점에_가깝다() {
            QuantFundamentalData f = new QuantFundamentalData(
                    BigDecimal.valueOf(35.0), BigDecimal.valueOf(4.0), null, BigDecimal.valueOf(50000));
            QuantFeatureResult result = calculator.calculate(buildInput(f));

            assertThat(result.normalizedScore()).isLessThan(10.0);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("fundamentals가 null이면 50점 중립 반환")
        void fundamentals_null이면_중립이다() {
            QuantFeatureResult result = calculator.calculate(buildInput(null));
            assertThat(result.normalizedScore()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("적자 기업 PER=0이면 50점 중립 반환")
        void 적자기업_PER_0이면_중립이다() {
            QuantFundamentalData f = new QuantFundamentalData(
                    BigDecimal.ZERO, BigDecimal.valueOf(1.0), null, BigDecimal.valueOf(50000));
            QuantFeatureResult result = calculator.calculate(buildInput(f));

            assertThat(result.normalizedScore()).isEqualTo(75.0); // PER=50(중립) + PBR=100(PBR≤1) / 2 = 75
        }

        @Test
        @DisplayName("점수는 항상 0~100 사이이다")
        void 점수는_0에서_100_사이이다() {
            QuantFundamentalData f = new QuantFundamentalData(
                    BigDecimal.valueOf(100.0), BigDecimal.valueOf(10.0), null, BigDecimal.valueOf(50000));
            QuantFeatureResult result = calculator.calculate(buildInput(f));

            assertThat(result.normalizedScore()).isBetween(0.0, 100.0);
        }
    }

    private QuantFeatureInput buildInput(QuantFundamentalData fundamentals) {
        return new QuantFeatureInput("005930", "삼성전자", MarketType.KOR,
                dummyPrices, List.of(), 0.0, fundamentals, 0.0);
    }
}
