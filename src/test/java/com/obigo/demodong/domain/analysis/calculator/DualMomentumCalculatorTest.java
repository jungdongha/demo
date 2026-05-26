package com.obigo.demodong.domain.analysis.calculator;

import com.obigo.demodong.domain.analysis.domain.calculator.DualMomentumCalculator;
import com.obigo.demodong.domain.analysis.domain.enums.MarketRegime;
import com.obigo.demodong.domain.analysis.domain.model.MomentumSnapshot;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DualMomentumCalculator 단위 테스트")
class DualMomentumCalculatorTest {

    private final DualMomentumCalculator calculator = new DualMomentumCalculator();

    @Test
    @DisplayName("강한 모멘텀 시나리오: 수익률과 RS Rating이 모두 높고 강세장인 경우 높은 점수를 반환한다")
    void 강한모멘텀_높은점수() {
        MomentumSnapshot momentum = new MomentumSnapshot(
                BigDecimal.valueOf(5.0),  // 1m
                BigDecimal.valueOf(7.0),  // 3m
                BigDecimal.valueOf(9.0),  // 6m
                BigDecimal.valueOf(11.0), // 12m -> 평균 8.0%
                90                        // rsRating
        );
        // 절대 모멘텀: (8.0 + 10) * 5 = 90
        // 상대 모멘텀: 90
        // 최종 점수: 90점
        StrategyInput input = new StrategyInput("TEST", MarketType.KOR, null, momentum, MarketRegime.STRONG_BULL, "반도체");
        StrategyScore score = calculator.calculate(input);

        assertThat(score.score()).isEqualTo(90);
        assertThat(score.grade()).isEqualTo("S");
        assertThat(score.positives()).contains("중장기 절대 모멘텀 우상향 (평균 수익률: 8.00%)");
    }

    @Test
    @DisplayName("약한 모멘텀 시나리오: 수익률과 RS Rating이 모두 낮고 하락장인 경우 0점을 반환한다")
    void 최약모멘텀_0점() {
        MomentumSnapshot momentum = new MomentumSnapshot(
                BigDecimal.valueOf(-10.0),
                BigDecimal.valueOf(-12.0),
                BigDecimal.valueOf(-8.0),
                BigDecimal.valueOf(-10.0), // 평균 -10.0% -> 절대 0점
                0                          // rsRating
        );
        StrategyInput input = new StrategyInput("TEST", MarketType.KOR, null, momentum, MarketRegime.BEAR, "바이오");
        StrategyScore score = calculator.calculate(input);

        assertThat(score.score()).isEqualTo(0);
        assertThat(score.grade()).isEqualTo("D");
    }

    @Test
    @DisplayName("데이터 부족 시나리오: 모멘텀 데이터가 null이면 중립(50점)을 반환한다")
    void 데이터부족_50점중립() {
        StrategyInput input = new StrategyInput("TEST", MarketType.KOR, null, null, MarketRegime.BULL, "IT");
        StrategyScore score = calculator.calculate(input);

        assertThat(score.score()).isEqualTo(50);
        assertThat(score.grade()).isEqualTo("B");
        assertThat(score.negatives()).contains("모멘텀 데이터 부족 — 중립 처리");
    }

    @Test
    @DisplayName("시장 국면 보정 - BEAR: BEAR 국면인 경우 절대 모멘텀 점수가 20% 감점된다")
    void 하락장보정_감점() {
        MomentumSnapshot momentum = new MomentumSnapshot(
                BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(10.0), // 평균 10.0% -> 절대 모멘텀 원본 점수 100점
                80                        // rsRating
        );
        // BEAR 국면 보정: 절대 점수 100 -> 80으로 감점 (20% 감점)
        // 최종 점수: 절대 80 * 0.5 + 상대 80 * 0.5 = 80점
        StrategyInput input = new StrategyInput("TEST", MarketType.KOR, null, momentum, MarketRegime.BEAR, "반도체");
        StrategyScore score = calculator.calculate(input);

        assertThat(score.score()).isEqualTo(80);
        assertThat(score.detail().get("absolute_momentum")).isEqualTo(80);
        assertThat(score.negatives()).contains("시장 국면 하락장(BEAR) 감점 반영 (-20%)");
    }

    @Test
    @DisplayName("시장 국면 보정 - CRISIS: CRISIS 국면인 경우 절대 모멘텀 점수가 50% 감점된다")
    void 급락장보정_감점() {
        MomentumSnapshot momentum = new MomentumSnapshot(
                BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(10.0), // 평균 10.0% -> 절대 모멘텀 원본 점수 100점
                50                        // rsRating
        );
        // CRISIS 국면 보정: 절대 점수 100 -> 50으로 감점 (50% 감점)
        // 최종 점수: 절대 50 * 0.5 + 상대 50 * 0.5 = 50점
        StrategyInput input = new StrategyInput("TEST", MarketType.KOR, null, momentum, MarketRegime.CRISIS, "IT");
        StrategyScore score = calculator.calculate(input);

        assertThat(score.score()).isEqualTo(50);
        assertThat(score.detail().get("absolute_momentum")).isEqualTo(50);
        assertThat(score.negatives()).contains("시장 국면 급락장(CRISIS) 감점 반영 (-50%)");
    }
}
