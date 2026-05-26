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
 * 평균 회귀(Mean Reversion) 전략 Calculator.
 *
 * <p>과매도 상태의 종목이 평균으로 돌아올 가능성을 점수화한다.</p>
 *
 * <pre>
 * 점수 구성:
 *   RSI 점수    (40%): RSI < 30 = 100 / 30~40 = 75 / 40~60 = 50 / 60~70 = 25 / > 70 = 0
 *   Bollinger   (40%): 현재가 위치를 밴드 내 역선형 변환 (하단 근처 = 100)
 *   이격률       (20%): SMA20 대비 음수(하회) → 높은 점수, ±10%p 기준 0~100
 * </pre>
 *
 * <p>지원 시장: KOR + USA</p>
 */
@Component
public class MeanReversionCalculator implements StrategyCalculator {

    private static final int NEUTRAL = 50;

    @Override
    public StrategyType getSupportedStrategy() {
        return StrategyType.MEAN_REVERSION;
    }

    @Override
    public StrategyScore calculate(StrategyInput input) {
        TechnicalSnapshot t = input.technical();
        Map<String, Integer> detail = new LinkedHashMap<>();
        List<String> positives = new ArrayList<>();
        List<String> negatives = new ArrayList<>();

        int rsiScore       = computeRsiScore(t.rsi14(), detail);
        int bollingerScore = computeBollingerScore(t.currentPrice(), t.bollingerUpper(), t.bollingerLower(), detail);
        int deviationScore = computeDeviationScore(t.deviationFromSma20(), detail);

        // 가중 합산: RSI 40% + Bollinger 40% + 이격률 20%
        int totalScore = (int) Math.round(rsiScore * 0.4 + bollingerScore * 0.4 + deviationScore * 0.2);
        totalScore = clamp(totalScore);

        buildMessages(rsiScore, bollingerScore, deviationScore, positives, negatives);

        return new StrategyScore(
                StrategyType.MEAN_REVERSION,
                totalScore,
                StrategyScore.gradeOf(totalScore),
                Map.copyOf(detail),
                List.copyOf(positives),
                List.copyOf(negatives)
        );
    }

    // ──────────────── 세부 점수 계산 ────────────────

    private int computeRsiScore(BigDecimal rsi, Map<String, Integer> detail) {
        if (rsi == null) {
            detail.put("rsi", NEUTRAL);
            return NEUTRAL;
        }
        double v = rsi.doubleValue();
        int score;
        if      (v < 30)  score = 100;
        else if (v < 40)  score = 75;
        else if (v <= 60) score = 50;
        else if (v <= 70) score = 25;
        else              score = 0;
        detail.put("rsi", score);
        return score;
    }

    /**
     * 볼린저 밴드 점수: 현재가가 밴드 내 어느 위치인지를 역선형 변환.
     * position = (price - lower) / (upper - lower) → 0(하단)~1(상단)
     * score = (1 - position) × 100
     */
    private int computeBollingerScore(BigDecimal currentPrice, BigDecimal upper,
                                      BigDecimal lower, Map<String, Integer> detail) {
        if (currentPrice == null || upper == null || lower == null) {
            detail.put("bollinger", NEUTRAL);
            return NEUTRAL;
        }
        double price = currentPrice.doubleValue();
        double u = upper.doubleValue();
        double l = lower.doubleValue();
        double bandWidth = u - l;
        if (bandWidth <= 0) {
            detail.put("bollinger", NEUTRAL);
            return NEUTRAL;
        }
        double position = (price - l) / bandWidth;
        int score = clamp((int) Math.round((1.0 - position) * 100));
        detail.put("bollinger", score);
        return score;
    }

    /**
     * 이격률 점수: SMA20 대비 음수(하회)일수록 높은 점수.
     * -10% → 100점, 0% → 50점, +10% → 0점 (선형)
     */
    private int computeDeviationScore(BigDecimal deviationFromSma20, Map<String, Integer> detail) {
        if (deviationFromSma20 == null) {
            detail.put("deviation", NEUTRAL);
            return NEUTRAL;
        }
        double dev = deviationFromSma20.doubleValue();
        // ±10%p 범위로 clamp 후 역선형 정규화
        dev = Math.max(-10.0, Math.min(10.0, dev));
        int score = clamp((int) Math.round(50.0 - dev * 5.0));
        detail.put("deviation", score);
        return score;
    }

    private void buildMessages(int rsiScore, int bollingerScore, int deviationScore,
                               List<String> positives, List<String> negatives) {
        if (rsiScore >= 75)        positives.add("RSI 과매도 구간 — 반등 가능성 높음");
        else if (rsiScore <= 25)   negatives.add("RSI 과매수 구간 — 하락 위험");

        if (bollingerScore >= 75)  positives.add("볼린저 하단 근처 — 매수 신호");
        else if (bollingerScore <= 25) negatives.add("볼린저 상단 근처 — 과열 경고");

        if (deviationScore >= 70)  positives.add("SMA20 하회 — 평균 회귀 기대");
        else if (deviationScore <= 30) negatives.add("SMA20 상회 — 이격률 과도");
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
