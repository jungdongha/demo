package com.obigo.demodong.domain.analysis.calculator;

import com.obigo.demodong.domain.analysis.domain.calculator.CANSLIMCalculator;
import com.obigo.demodong.domain.analysis.domain.enums.MarketRegime;
import com.obigo.demodong.domain.analysis.domain.model.MomentumSnapshot;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.flow.domain.model.FlowSnapshot;
import com.obigo.demodong.domain.fundamental.domain.model.FundamentalSnapshot;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.technical.domain.model.TechnicalSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CANSLIMCalculator 단위 테스트")
class CANSLIMCalculatorTest {

    private final CANSLIMCalculator calculator = new CANSLIMCalculator();

    // ──────────────────────────────────────────────────────────────────────
    //  최적 / 최악 시나리오
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("점수 시나리오")
    class 점수시나리오 {

        @Test
        @DisplayName("모든 조건 최적일 때 85점 이상 S 등급을 반환한다")
        void 최적_조건_S등급() {
            StrategyScore score = calculator.calculate(buildOptimalInput());

            assertThat(score.score()).isGreaterThanOrEqualTo(85);
            assertThat(score.grade()).isEqualTo("S");
            assertThat(score.positives()).isNotEmpty();
        }

        @Test
        @DisplayName("모든 조건 최악일 때 15점 이하 D 등급을 반환한다")
        void 최악_조건_D등급() {
            StrategyScore score = calculator.calculate(buildWorstInput());

            assertThat(score.score()).isLessThanOrEqualTo(15);
            assertThat(score.grade()).isEqualTo("D");
            assertThat(score.negatives()).isNotEmpty();
        }

        @Test
        @DisplayName("점수는 반드시 0~100 범위이다")
        void 점수_범위_검증() {
            StrategyScore optimal = calculator.calculate(buildOptimalInput());
            StrategyScore worst   = calculator.calculate(buildWorstInput());

            assertThat(optimal.score()).isBetween(0, 100);
            assertThat(worst.score()).isBetween(0, 100);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  null / stub 처리
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("null / stub 처리")
    class NullStub처리 {

        @Test
        @DisplayName("flow null이면 S/I 항목 50점 중립으로 계산된다")
        void flow_null_SI항목_중립() {
            // flow=null, 나머지는 optimal
            StrategyInput input = new StrategyInput(
                    "005930", MarketType.KOR,
                    buildOptimalTechnical(),
                    buildOptimalMomentum(),
                    MarketRegime.STRONG_BULL,
                    "IT",
                    buildOptimalFundamental(),
                    null   // flow null
            );
            StrategyScore score = calculator.calculate(input);

            assertThat(score.detail().get("S_supply_demand")).isEqualTo(50);
            assertThat(score.detail().get("I_institutional_buy")).isEqualTo(50);
        }

        @Test
        @DisplayName("FlowSnapshot.stub()이면 S/I 항목 50점 중립으로 계산된다")
        void flow_stub_SI항목_중립() {
            StrategyInput input = new StrategyInput(
                    "005930", MarketType.KOR,
                    buildOptimalTechnical(),
                    buildOptimalMomentum(),
                    MarketRegime.BULL,
                    "반도체",
                    buildOptimalFundamental(),
                    FlowSnapshot.stub()
            );
            StrategyScore score = calculator.calculate(input);

            assertThat(score.detail().get("S_supply_demand")).isEqualTo(50);
            assertThat(score.detail().get("I_institutional_buy")).isEqualTo(50);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  항목별 점수 검증
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("항목별 점수 검증")
    class 항목별점수검증 {

        @Test
        @DisplayName("기관+외국인 동반 순매수이면 S 항목 100점이다")
        void 기관_외국인_동반순매수_S100() {
            FlowSnapshot flow = new FlowSnapshot(500_000L, 300_000L, -200_000L);
            StrategyInput input = new StrategyInput(
                    "005930", MarketType.KOR,
                    null, null, null, null, null, flow
            );
            StrategyScore score = calculator.calculate(input);

            assertThat(score.detail().get("S_supply_demand")).isEqualTo(100);
        }

        @Test
        @DisplayName("RS Rating 100 + STRONG_BULL이면 L/M 항목 최고점이다")
        void RS100_STRONG_BULL_LM최고점() {
            MomentumSnapshot momentum = new MomentumSnapshot(
                    bd(5.0), bd(15.0), bd(30.0), bd(50.0), 100
            );
            StrategyInput input = new StrategyInput(
                    "005930", MarketType.KOR,
                    null, momentum, MarketRegime.STRONG_BULL, null, null, null
            );
            StrategyScore score = calculator.calculate(input);

            assertThat(score.detail().get("L_rs_rating")).isEqualTo(100);
            assertThat(score.detail().get("M_market_regime")).isEqualTo(100);
        }

        @Test
        @DisplayName("52주 신고가 5% 이내이면 N 항목 100점이다")
        void 신고가_5퍼센트이내_N100() {
            // currentPrice = 95, high52w = 100 → ratio = 0.95
            TechnicalSnapshot tech = buildTechnicalWithPrices(bd(95.0), bd(100.0));
            StrategyInput input = new StrategyInput(
                    "005930", MarketType.KOR,
                    tech, null, null, null, null, null
            );
            StrategyScore score = calculator.calculate(input);

            assertThat(score.detail().get("N_new_high_proximity")).isEqualTo(100);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  헬퍼
    // ──────────────────────────────────────────────────────────────────────

    private StrategyInput buildOptimalInput() {
        return new StrategyInput(
                "005930", MarketType.KOR,
                buildOptimalTechnical(),
                buildOptimalMomentum(),
                MarketRegime.STRONG_BULL,
                "반도체",
                buildOptimalFundamental(),
                new FlowSnapshot(2_000_000L, 1_000_000L, -500_000L)  // 기관+외인 동반 대규모 순매수
        );
    }

    private StrategyInput buildWorstInput() {
        FundamentalSnapshot f = new FundamentalSnapshot(
                null, null, null, null,
                null,
                bd(-5.0), null,   // roa 음수
                null, null, null, null, null, null, null, null, null, null,
                bd(-10.0),        // revenueGrowthYoy 음수
                null, null,
                null, null, null  // roic, earningsYield, operatingProfit (Phase 6)
        );
        // currentPrice = 50, high52w = 100 → ratio = 0.50 → 0점
        TechnicalSnapshot tech = buildTechnicalWithPrices(bd(50.0), bd(100.0));
        MomentumSnapshot momentum = new MomentumSnapshot(
                bd(-5.0), bd(-10.0), bd(-15.0), bd(-20.0), 10
        );
        FlowSnapshot flow = new FlowSnapshot(-2_000_000L, -1_000_000L, 500_000L);  // 기관+외인 동반 매도

        return new StrategyInput(
                "999999", MarketType.KOR,
                tech, momentum, MarketRegime.CRISIS, "테스트", f, flow
        );
    }

    private FundamentalSnapshot buildOptimalFundamental() {
        return new FundamentalSnapshot(
                null, null, null, null,
                null,
                bd(25.0),   // roa > 20% → A 100점
                bd(20.0),
                null, null, null, null, null, null, null, null, null, null,
                bd(30.0),   // revenueGrowthYoy ≥ 25% → C 100점
                null, null,
                null, null, null  // roic, earningsYield, operatingProfit (Phase 6)
        );
    }

    private TechnicalSnapshot buildOptimalTechnical() {
        return buildTechnicalWithPrices(bd(97.0), bd(100.0)); // 신고가 3% 이내 → N 100점
    }

    private TechnicalSnapshot buildTechnicalWithPrices(BigDecimal currentPrice, BigDecimal high52w) {
        return new TechnicalSnapshot(
                null, null, null, null,
                null, null, null,
                null, null, null, null, null, null,
                currentPrice,
                high52w,
                null, null
        );
    }

    private MomentumSnapshot buildOptimalMomentum() {
        return new MomentumSnapshot(
                bd(5.0), bd(15.0), bd(30.0), bd(50.0), 95  // RS Rating 95 → L 95점
        );
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }
}
