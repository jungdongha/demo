package com.obigo.demodong.domain.analysis.calculator;

import com.obigo.demodong.domain.analysis.domain.calculator.PiotroskiCalculator;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.fundamental.domain.model.FundamentalSnapshot;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PiotroskiCalculator 단위 테스트")
class PiotroskiCalculatorTest {

    private final PiotroskiCalculator calculator = new PiotroskiCalculator();

    // ──────────────────────────────────────────────────────────────────────
    //  F-Score 만점 / 전패 / 중간
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("F-Score 점수 시나리오")
    class FScore점수시나리오 {

        @Test
        @DisplayName("F9/9 모든 조건 통과 시 95점 이상 S 등급을 반환한다")
        void 만점_F9_점수95이상() {
            FundamentalSnapshot f = buildPerfect();
            StrategyScore score = calculator.calculate(buildInput(f));

            assertThat(score.score()).isGreaterThanOrEqualTo(95);
            assertThat(score.grade()).isEqualTo("S");
            assertThat(score.detail().get("f_score_raw")).isEqualTo(9);
            assertThat(score.positives()).hasSize(9);
            assertThat(score.negatives()).isEmpty();
        }

        @Test
        @DisplayName("F0/9 모든 조건 실패 시 0점 D 등급을 반환한다")
        void 전패_F0_0점() {
            FundamentalSnapshot f = buildWorst();
            StrategyScore score = calculator.calculate(buildInput(f));

            assertThat(score.score()).isEqualTo(0);
            assertThat(score.grade()).isEqualTo("D");
            assertThat(score.detail().get("f_score_raw")).isEqualTo(0);
            assertThat(score.negatives()).hasSize(9);
        }

        @Test
        @DisplayName("F5/9 중간 점수 시 50~60점 B 등급을 반환한다")
        void 중간_F5_B등급() {
            // F1(ROA>0) O, F2(CFO>0) O, F3(ROA 개선) O, F4(부채감소) O, F5(유동비율개선) O
            // F6(신주미발행) X, F7(GPM개선) X, F8(회전율개선) X, F9(CFO>ROA) X
            FundamentalSnapshot f = new FundamentalSnapshot(
                    null, null, null, null,
                    null,
                    bd(5.0),   // roa (+, 개선됨)
                    bd(3.0),   // prevRoa
                    bd(1_000_000_000L), // operatingCashFlow (+)
                    bd(20_000_000_000L), // totalAssets
                    bd(40.0),  // debtRatio (감소)
                    bd(50.0),  // prevDebtRatio
                    bd(1.8),   // currentRatio (개선)
                    bd(1.5),   // prevCurrentRatio
                    bd(28.0),  // grossProfitMargin (악화됨)
                    bd(30.0),  // prevGrossProfitMargin
                    bd(0.40),  // assetTurnover (악화됨)
                    bd(0.45),  // prevAssetTurnover
                    null,
                    1_100_000_000L,  // sharesOutstanding (증가 = 희석)
                    1_000_000_000L,  // prevSharesOutstanding
                    null, null, null // roic, earningsYield, operatingProfit (Phase 6)
            );
            StrategyScore score = calculator.calculate(buildInput(f));

            assertThat(score.detail().get("f_score_raw")).isEqualTo(5);
            assertThat(score.score()).isBetween(50, 60);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  null / stub 처리
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("null / stub 처리")
    class NullStub처리 {

        @Test
        @DisplayName("fundamental이 null이면 50점 중립을 반환한다")
        void fundamental_null_50점중립() {
            StrategyInput input = new StrategyInput("TEST", MarketType.KOR, null,
                    null, null, null, null);
            StrategyScore score = calculator.calculate(input);

            assertThat(score.score()).isEqualTo(50);
            assertThat(score.grade()).isEqualTo("B");
        }

        @Test
        @DisplayName("FundamentalSnapshot.stub()이면 50점 중립을 반환한다")
        void stub_50점중립() {
            StrategyInput input = buildInput(FundamentalSnapshot.stub());
            StrategyScore score = calculator.calculate(input);

            assertThat(score.score()).isEqualTo(50);
        }

        @Test
        @DisplayName("미국 주식처럼 일부 핵심 필드만 null이면 해당 항목 0점으로 나머지 정상 계산한다")
        void 일부필드null_해당항목0점() {
            // ROA, prevROA 만 null → F1, F3 은 0점, 나머지 7개 중 통과 가능
            FundamentalSnapshot f = new FundamentalSnapshot(
                    null, null, null, null,
                    null,
                    null,       // roa — null → F1, F9 0점
                    null,       // prevRoa — null → F3 0점
                    bd(500_000_000L), // operatingCashFlow (+) → F2 1점
                    bd(10_000_000_000L), // totalAssets
                    bd(30.0),   // debtRatio (감소) → F4 1점
                    bd(40.0),   // prevDebtRatio
                    bd(2.0),    // currentRatio (개선) → F5 1점
                    bd(1.5),    // prevCurrentRatio
                    bd(35.0),   // grossProfitMargin (개선) → F7 1점
                    bd(30.0),   // prevGrossProfitMargin
                    bd(0.5),    // assetTurnover (개선) → F8 1점
                    bd(0.4),    // prevAssetTurnover
                    null,
                    900_000_000L,  // sharesOutstanding (감소) → F6 1점
                    1_000_000_000L, // prevSharesOutstanding
                    null, null, null // roic, earningsYield, operatingProfit (Phase 6)
            );
            StrategyScore score = calculator.calculate(buildInput(f));

            // F1(0) + F2(1) + F3(0) + F4(1) + F5(1) + F6(1) + F7(1) + F8(1) + F9(0) = 6
            assertThat(score.detail().get("f_score_raw")).isEqualTo(6);
            assertThat(score.detail().get("F1_roa_positive")).isEqualTo(0);
            assertThat(score.detail().get("F3_roa_improving")).isEqualTo(0);
            assertThat(score.detail().get("F2_cfo_positive")).isEqualTo(1);
        }

        @Test
        @DisplayName("점수는 반드시 0~100 범위이다")
        void 점수범위검증() {
            StrategyScore perfect = calculator.calculate(buildInput(buildPerfect()));
            StrategyScore worst   = calculator.calculate(buildInput(buildWorst()));

            assertThat(perfect.score()).isBetween(0, 100);
            assertThat(worst.score()).isBetween(0, 100);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  헬퍼
    // ──────────────────────────────────────────────────────────────────────

    /**
     * F9/9 만점 스냅샷: 모든 조건 통과.
     * F9 검증: CFO/총자산 = 2.1B/25B = 0.084 > ROA/100 = 8.0/100 = 0.08 ✓
     */
    private FundamentalSnapshot buildPerfect() {
        return new FundamentalSnapshot(
                null, null, null, null,
                null,
                bd(8.0),              // roa (+)
                bd(5.0),              // prevRoa (개선됨)
                bd(2_100_000_000L),   // operatingCashFlow → CFO/총자산 = 0.084 > ROA/100 = 0.08
                bd(25_000_000_000L),  // totalAssets
                bd(35.0),             // debtRatio (감소)
                bd(45.0),             // prevDebtRatio
                bd(2.5),              // currentRatio (개선)
                bd(1.8),              // prevCurrentRatio
                bd(40.0),             // grossProfitMargin (개선)
                bd(35.0),             // prevGrossProfitMargin
                bd(0.55),             // assetTurnover (개선)
                bd(0.48),             // prevAssetTurnover
                null,
                900_000_000L,         // sharesOutstanding (감소 = 미발행)
                1_000_000_000L,       // prevSharesOutstanding
                null, null, null      // roic, earningsYield, operatingProfit (Phase 6)
        );
    }

    /** F0/9 전패 스냅샷: 모든 조건 실패 */
    private FundamentalSnapshot buildWorst() {
        return new FundamentalSnapshot(
                null, null, null, null,
                null,
                bd(-3.0),   // roa 음수 → F1 실패
                bd(-1.0),   // prevRoa (악화됨) → F3 실패
                bd(-500_000_000L), // operatingCashFlow 음수 → F2 실패
                bd(10_000_000_000L), // totalAssets
                bd(65.0),   // debtRatio (증가) → F4 실패
                bd(50.0),   // prevDebtRatio
                bd(0.8),    // currentRatio (악화) → F5 실패
                bd(1.2),    // prevCurrentRatio
                bd(20.0),   // grossProfitMargin (악화) → F7 실패
                bd(25.0),   // prevGrossProfitMargin
                bd(0.3),    // assetTurnover (악화) → F8 실패
                bd(0.4),    // prevAssetTurnover
                null,
                1_200_000_000L,  // sharesOutstanding (증가 = 희석) → F6 실패
                1_000_000_000L,  // prevSharesOutstanding
                // F9: CFO/총자산 = -500M/10B = -0.05 > ROA/100 = -0.03 → -0.05 < -0.03 → 실패
                null, null, null // roic, earningsYield, operatingProfit (Phase 6)
        );
    }

    private StrategyInput buildInput(FundamentalSnapshot fundamental) {
        return new StrategyInput("005930", MarketType.KOR, null,
                null, null, null, fundamental);
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }

    private BigDecimal bd(long value) {
        return BigDecimal.valueOf(value);
    }
}
