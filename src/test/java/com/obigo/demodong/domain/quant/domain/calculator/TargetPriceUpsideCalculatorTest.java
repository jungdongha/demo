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

@DisplayName("TargetPriceUpsideCalculator 단위 테스트")
class TargetPriceUpsideCalculatorTest {

    private TargetPriceUpsideCalculator calculator;
    private List<DailyQuote> dummyPrices;

    @BeforeEach
    void setUp() {
        calculator = new TargetPriceUpsideCalculator();
        dummyPrices = List.of(new DailyQuote(LocalDate.now(), BigDecimal.valueOf(50000), 1_000_000L));
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("목표주가 상승 여력 30%이면 100점 반환 (만점 기준)")
        void 상승여력_30퍼센트이면_100점이다() {
            // currentPrice=50000, targetPrice=65000 → upside=30%
            QuantFundamentalData f = new QuantFundamentalData(
                    null, null, BigDecimal.valueOf(65000), BigDecimal.valueOf(50000));
            QuantFeatureResult result = calculator.calculate(buildInput(f));

            assertThat(result.normalizedScore()).isEqualTo(100.0);
            assertThat(result.rawValue()).isCloseTo(30.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("목표주가 상승 여력 0%이면 0점 반환")
        void 상승여력_0퍼센트이면_0점이다() {
            // currentPrice = targetPrice → upside=0%
            QuantFundamentalData f = new QuantFundamentalData(
                    null, null, BigDecimal.valueOf(50000), BigDecimal.valueOf(50000));
            QuantFeatureResult result = calculator.calculate(buildInput(f));

            assertThat(result.normalizedScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("목표주가 상승 여력 15%이면 50점 반환")
        void 상승여력_15퍼센트이면_50점이다() {
            // currentPrice=50000, targetPrice=57500 → upside=15%
            QuantFundamentalData f = new QuantFundamentalData(
                    null, null, BigDecimal.valueOf(57500), BigDecimal.valueOf(50000));
            QuantFeatureResult result = calculator.calculate(buildInput(f));

            assertThat(result.normalizedScore()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(1.0));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("목표주가가 null(Stub)이면 50점 중립 반환")
        void 목표주가_null이면_중립이다() {
            QuantFundamentalData f = QuantFundamentalData.stub(BigDecimal.valueOf(50000));
            QuantFeatureResult result = calculator.calculate(buildInput(f));

            assertThat(result.normalizedScore()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("fundamentals 자체가 null이면 50점 중립 반환")
        void fundamentals_null이면_중립이다() {
            QuantFeatureResult result = calculator.calculate(buildInput(null));
            assertThat(result.normalizedScore()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("목표주가가 현재가보다 낮으면(하향) 0점 반환")
        void 목표주가_하향이면_0점이다() {
            // currentPrice=50000, targetPrice=40000 → upside=-20%
            QuantFundamentalData f = new QuantFundamentalData(
                    null, null, BigDecimal.valueOf(40000), BigDecimal.valueOf(50000));
            QuantFeatureResult result = calculator.calculate(buildInput(f));

            assertThat(result.normalizedScore()).isEqualTo(0.0);
        }
    }

    private QuantFeatureInput buildInput(QuantFundamentalData fundamentals) {
        return new QuantFeatureInput("005930", "삼성전자", MarketType.KOR,
                dummyPrices, List.of(), 0.0, fundamentals, 0.0);
    }
}
