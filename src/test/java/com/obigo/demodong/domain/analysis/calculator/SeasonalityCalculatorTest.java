package com.obigo.demodong.domain.analysis.calculator;

import com.obigo.demodong.domain.analysis.domain.calculator.SeasonalityCalculator;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.analysis.infrastructure.SeasonalityTable;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SeasonalityCalculator 단위 테스트")
class SeasonalityCalculatorTest {

    private SeasonalityTable seasonalityTable;
    private TestSeasonalityCalculator calculator;

    // 테스트를 위한 Month 오버라이드 헬퍼 클래스
    private static class TestSeasonalityCalculator extends SeasonalityCalculator {
        private Month testMonth = Month.JANUARY;

        public TestSeasonalityCalculator(SeasonalityTable table) {
            super(table);
        }

        public void setTestMonth(Month month) {
            this.testMonth = month;
        }

        @Override
        protected Month getCurrentMonth() {
            return testMonth;
        }
    }

    @BeforeEach
    void setUp() {
        seasonalityTable = new SeasonalityTable();
        calculator = new TestSeasonalityCalculator(seasonalityTable);
    }

    @Test
    @DisplayName("반도체 연말 강세 시나리오: 12월 반도체 섹터는 90점을 반환하고 강세 메시지를 포함한다")
    void 반도체_12월_강세() {
        calculator.setTestMonth(Month.DECEMBER);
        StrategyInput input = new StrategyInput("005930", MarketType.KOR, null, null, null, "반도체");
        StrategyScore score = calculator.calculate(input);

        assertThat(score.score()).isEqualTo(90);
        assertThat(score.positives()).contains("반도체 업종 12월 계절적 강세 진입 (계절성 점수: 90)");
    }

    @Test
    @DisplayName("바이오 봄철 강세 시나리오: 4월 바이오 섹터는 85점을 반환한다")
    void 바이오_4월_강세() {
        calculator.setTestMonth(Month.APRIL);
        StrategyInput input = new StrategyInput("068270", MarketType.KOR, null, null, null, "바이오");
        StrategyScore score = calculator.calculate(input);

        assertThat(score.score()).isEqualTo(85);
        assertThat(score.positives()).contains("바이오 업종 4월 계절적 강세 진입 (계절성 점수: 85)");
    }

    @Test
    @DisplayName("은행 봄철 약세 시나리오: 4월 은행 섹터는 40점을 반환하고 약세 메시지를 포함한다")
    void 은행_4월_약세() {
        calculator.setTestMonth(Month.APRIL);
        StrategyInput input = new StrategyInput("055550", MarketType.KOR, null, null, null, "은행");
        StrategyScore score = calculator.calculate(input);

        assertThat(score.score()).isEqualTo(40);
        assertThat(score.negatives()).contains("은행 업종 4월 계절적 약세 진입 (계절성 점수: 40)");
    }

    @Test
    @DisplayName("미분류 섹터 시나리오: sector가 null이거나 없는 경우 DEFAULT 테이블을 사용하여 12월 80점을 반환한다")
    void 미분류섹터_DEFAULT_12월() {
        calculator.setTestMonth(Month.DECEMBER);
        StrategyInput input1 = new StrategyInput("TEST1", MarketType.KOR, null, null, null, null);
        StrategyScore score1 = calculator.calculate(input1);

        StrategyInput input2 = new StrategyInput("TEST2", MarketType.KOR, null, null, null, "우주항공");
        StrategyScore score2 = calculator.calculate(input2);

        // DEFAULT 12월 점수 = 80
        assertThat(score1.score()).isEqualTo(80);
        assertThat(score2.score()).isEqualTo(80);
        assertThat(score1.positives()).contains("시장 전체 업종 12월 계절적 강세 진입 (계절성 점수: 80)");
    }

    @Test
    @DisplayName("DEFAULT 봄철 약세 시나리오: 미분류 섹터 5월 평가는 40점을 반환한다")
    void DEFAULT_5월_약세() {
        calculator.setTestMonth(Month.MAY);
        StrategyInput input = new StrategyInput("TEST", MarketType.KOR, null, null, null, null);
        StrategyScore score = calculator.calculate(input);

        // DEFAULT 5월 점수 = 40
        assertThat(score.score()).isEqualTo(40);
        assertThat(score.negatives()).contains("시장 전체 업종 5월 계절적 약세 진입 (계절성 점수: 40)");
    }
}
