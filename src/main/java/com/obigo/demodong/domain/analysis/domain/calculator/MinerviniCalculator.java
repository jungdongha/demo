package com.obigo.demodong.domain.analysis.domain.calculator;

import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.technical.domain.model.TechnicalSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 미네르비니(Minervini) 추세 전략 Calculator.
 *
 * <p>마크 미네르비니의 SEPA(Specific Entry Point Analysis) 8조건 중
 * Phase 2에서 데이터가 있는 7조건을 평가한다.
 * RS Rating(조건8)은 Phase 3 FlowSnapshot 연동 후 추가 예정.</p>
 *
 * <pre>
 * 7조건:
 *   1. 현재가 > EMA150 > EMA200
 *   2. EMA200 최근 20거래일 우상향
 *   3. EMA50 > EMA150 > EMA200 (정배열)
 *   4. 현재가 > EMA50
 *   5. 현재가 > EMA20
 *   6. 현재가 ≥ 52주 저점 × 1.30
 *   7. 현재가 ≥ 52주 고점 × 0.75
 *
 * 점수 = 충족 조건 수 / 유효 조건 수 × 100
 * 데이터가 없는 조건은 유효 조건 수에서 제외.
 * </pre>
 *
 * <p>지원 시장: KOR + USA</p>
 */
@Component
public class MinerviniCalculator implements StrategyCalculator {

    private static final int NEUTRAL = 50;
    private static final BigDecimal LOW_52W_MULTIPLIER  = new BigDecimal("1.30");
    private static final BigDecimal HIGH_52W_MULTIPLIER = new BigDecimal("0.75");

    @Override
    public StrategyType getSupportedStrategy() {
        return StrategyType.MINERVINI;
    }

    @Override
    public StrategyScore calculate(StrategyInput input) {
        TechnicalSnapshot t = input.technical();

        if (t == null || t.currentPrice() == null || t.emaMap() == null || t.emaMap().isEmpty()) {
            return neutralScore("기술적 데이터 부족 — 중립 처리");
        }

        BigDecimal price   = t.currentPrice();
        BigDecimal ema20   = t.emaMap().get(20);
        BigDecimal ema50   = t.emaMap().get(50);
        BigDecimal ema150  = t.emaMap().get(150);
        BigDecimal ema200  = t.emaMap().get(200);
        BigDecimal high52w = t.high52w();
        BigDecimal low52w  = t.low52w();
        BigDecimal ema200Prev = t.ema200OneMonthAgo();

        Map<String, Integer> detail = new LinkedHashMap<>();
        List<String> positives = new ArrayList<>();
        List<String> negatives = new ArrayList<>();

        int metCount = 0;
        int totalConditions = 8;

        // 조건 1: 현재가 > EMA150 > EMA200
        boolean c1 = ema150 != null && ema200 != null
                && price.compareTo(ema150) > 0
                && ema150.compareTo(ema200) > 0;
        detail.put("c1_price_ema150_ema200", c1 ? 1 : 0);
        if (c1) {
            metCount++;
            positives.add("현재가 > EMA150 > EMA200 충족");
        } else {
            negatives.add("현재가 > EMA150 > EMA200 미충족");
        }

        // 조건 2: EMA200 최근 20거래일 우상향
        if (ema200Prev == null) {
            totalConditions--;
            // 데이터 없음 → detail 제외
        } else {
            boolean c2 = ema200 != null && ema200.compareTo(ema200Prev) > 0;
            detail.put("c2_ema200_uptrend", c2 ? 1 : 0);
            if (c2) {
                metCount++;
                positives.add("EMA200 20거래일 우상향");
            } else {
                negatives.add("EMA200 우상향 미확인");
            }
        }

        // 조건 3: EMA50 > EMA150 > EMA200 (정배열)
        boolean c3 = ema50 != null && ema150 != null && ema200 != null
                && ema50.compareTo(ema150) > 0
                && ema150.compareTo(ema200) > 0;
        detail.put("c3_ema_alignment", c3 ? 1 : 0);
        if (c3) {
            metCount++;
            positives.add("EMA 정배열 (50 > 150 > 200) 달성");
        } else {
            negatives.add("EMA 정배열 미충족");
        }

        // 조건 4: 현재가 > EMA50
        boolean c4 = ema50 != null && price.compareTo(ema50) > 0;
        detail.put("c4_price_above_ema50", c4 ? 1 : 0);
        if (c4) metCount++;

        // 조건 5: 현재가 > EMA20
        boolean c5 = ema20 != null && price.compareTo(ema20) > 0;
        detail.put("c5_price_above_ema20", c5 ? 1 : 0);
        if (c5) metCount++;

        // 조건 6: 현재가 ≥ 52주 저점 × 1.30
        if (low52w == null) {
            totalConditions--;
        } else {
            boolean c6 = price.compareTo(low52w.multiply(LOW_52W_MULTIPLIER)) >= 0;
            detail.put("c6_above_52w_low_130pct", c6 ? 1 : 0);
            if (c6) {
                metCount++;
                positives.add("52주 저점 대비 +30% 이상 회복");
            } else {
                negatives.add("52주 저점 대비 +30% 미달");
            }
        }

        // 조건 7: 현재가 ≥ 52주 고점 × 0.75
        if (high52w == null) {
            totalConditions--;
        } else {
            boolean c7 = price.compareTo(high52w.multiply(HIGH_52W_MULTIPLIER)) >= 0;
            detail.put("c7_within_52w_high_75pct", c7 ? 1 : 0);
            if (c7) {
                metCount++;
                positives.add("52주 고점 25% 이내 근접 — 강한 추세");
            } else {
                negatives.add("52주 고점 대비 25% 이상 하락");
            }
        }

        // 조건 8: RS Rating ≥ 70
        if (input.momentum() == null) {
            totalConditions--;
        } else {
            boolean c8 = input.momentum().rsRating() >= 70;
            detail.put("c8_rs_rating", c8 ? 1 : 0);
            if (c8) {
                metCount++;
                positives.add("RS Rating 70 이상 — 시장 대비 강세");
            } else {
                negatives.add("RS Rating 70 미만 — 시장 대비 약세");
            }
        }

        // 점수: 충족 수 / 유효 조건 수 × 100
        detail.put("조건충족수", metCount);
        detail.put("유효조건수", totalConditions);

        int score = totalConditions > 0
                ? clamp((int) Math.round((double) metCount / totalConditions * 100))
                : NEUTRAL;

        return new StrategyScore(
                StrategyType.MINERVINI,
                score,
                StrategyScore.gradeOf(score),
                Map.copyOf(detail),
                List.copyOf(positives),
                List.copyOf(negatives)
        );
    }

    private StrategyScore neutralScore(String reason) {
        return new StrategyScore(
                StrategyType.MINERVINI,
                NEUTRAL,
                StrategyScore.gradeOf(NEUTRAL),
                Map.of("조건충족수", NEUTRAL),
                List.of(),
                List.of(reason)
        );
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
