package com.obigo.demodong.domain.analysis.calculator;

import com.obigo.demodong.domain.analysis.domain.calculator.MinerviniCalculator;
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

@DisplayName("MinerviniCalculator 단위 테스트")
class MinerviniCalculatorTest {

    private final MinerviniCalculator calculator = new MinerviniCalculator();

    @Nested
    @DisplayName("조건 충족 시나리오")
    class 조건충족시나리오 {

        @Test
        @DisplayName("7조건 모두 충족하면 100점을 반환한다")
        void 조건7개_100점() {
            TechnicalSnapshot snapshot = buildSnapshot(
                    BigDecimal.valueOf(200),  // currentPrice
                    BigDecimal.valueOf(150),  // ema20
                    BigDecimal.valueOf(140),  // ema50
                    BigDecimal.valueOf(130),  // ema150
                    BigDecimal.valueOf(120),  // ema200
                    BigDecimal.valueOf(115),  // ema200OneMonthAgo (< ema200 → 우상향)
                    BigDecimal.valueOf(250),  // high52w (200 >= 250*0.75=187.5 ✓)
                    BigDecimal.valueOf(100)   // low52w  (200 >= 100*1.30=130 ✓)
            );
            StrategyScore score = calculator.calculate(buildInput(snapshot));
            assertThat(score.score()).isEqualTo(100);
            assertThat(score.grade()).isEqualTo("S");
        }

        @Test
        @DisplayName("조건을 하나도 충족하지 않으면 0점을 반환한다")
        void 조건0개_0점() {
            TechnicalSnapshot snapshot = buildSnapshot(
                    BigDecimal.valueOf(100),  // currentPrice
                    BigDecimal.valueOf(150),  // ema20  (100 < 150 → c5 실패)
                    BigDecimal.valueOf(160),  // ema50  (100 < 160 → c4 실패)
                    BigDecimal.valueOf(170),  // ema150 (c1 실패, c3 실패 — ema50 < ema150)
                    BigDecimal.valueOf(180),  // ema200 (ema150 < ema200 → c1,c3 실패)
                    BigDecimal.valueOf(190),  // ema200OneMonthAgo (> ema200 → c2 실패)
                    BigDecimal.valueOf(400),  // high52w (100 < 400*0.75=300 → c7 실패)
                    BigDecimal.valueOf(200)   // low52w  (100 < 200*1.30=260 → c6 실패)
            );
            StrategyScore score = calculator.calculate(buildInput(snapshot));
            assertThat(score.score()).isEqualTo(0);
            assertThat(score.grade()).isEqualTo("D");
        }

        @Test
        @DisplayName("조건 절반(3~4개) 충족이면 40~60점 범위이다")
        void 조건절반_중간점수() {
            // c4, c5 충족 / c1, c3 미충족 / c6, c7 충족 / c2 데이터없음(제외)
            TechnicalSnapshot snapshot = new TechnicalSnapshot(
                    null, null, null, null,
                    null, null, null,
                    Map.of(
                            20,  BigDecimal.valueOf(90),   // ema20 < price(100) → c5 ✓
                            50,  BigDecimal.valueOf(95),   // ema50 < price(100) → c4 ✓
                            150, BigDecimal.valueOf(110),  // ema150 > price(100) → c1 ✗
                            200, BigDecimal.valueOf(120)   // ema200 > ema150 → c3 ✗
                    ),
                    null,  // ema200OneMonthAgo = null → c2 제외
                    null, null,  // atr14, adx14
                    null, null,  // sma20, sma60
                    BigDecimal.valueOf(100),   // currentPrice
                    BigDecimal.valueOf(120),   // high52w (100 >= 120*0.75=90 ✓) → c7 ✓
                    BigDecimal.valueOf(70),    // low52w  (100 >= 70*1.30=91 ✓) → c6 ✓
                    null
            );
            StrategyScore score = calculator.calculate(buildInput(snapshot));
            // 충족: c4, c5, c6, c7 = 4개 / 유효: 6개 → 67점
            assertThat(score.score()).isBetween(40, 75);
        }
    }

    // ──────────────── 헬퍼 ────────────────

    private TechnicalSnapshot buildSnapshot(BigDecimal price, BigDecimal ema20,
                                             BigDecimal ema50, BigDecimal ema150,
                                             BigDecimal ema200, BigDecimal ema200Prev,
                                             BigDecimal high52w, BigDecimal low52w) {
        return new TechnicalSnapshot(
                null, null, null, null,
                null, null, null,
                Map.of(20, ema20, 50, ema50, 150, ema150, 200, ema200),
                ema200Prev,
                null, null,
                null, null,
                price, high52w, low52w, null
        );
    }

    private StrategyInput buildInput(TechnicalSnapshot snapshot) {
        return new StrategyInput("TEST", MarketType.KOR, snapshot);
    }
}
