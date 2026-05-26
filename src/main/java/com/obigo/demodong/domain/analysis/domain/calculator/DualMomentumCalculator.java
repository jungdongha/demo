package com.obigo.demodong.domain.analysis.domain.calculator;

import com.obigo.demodong.domain.analysis.domain.enums.MarketRegime;
import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;
import com.obigo.demodong.domain.analysis.domain.model.MomentumSnapshot;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 듀얼 모멘텀(Dual Momentum) 전략 Calculator.
 * 절대 모멘텀(추세 상승 여부)과 상대 모멘텀(시장 대비 강세 여부)을 결합하여 평가한다.
 */
@Component
public class DualMomentumCalculator implements StrategyCalculator {

    private static final int NEUTRAL = 50;

    @Override
    public StrategyType getSupportedStrategy() {
        return StrategyType.DUAL_MOMENTUM;
    }

    @Override
    public StrategyScore calculate(StrategyInput input) {
        MomentumSnapshot m = input.momentum();
        Map<String, Integer> detail = new LinkedHashMap<>();
        List<String> positives = new ArrayList<>();
        List<String> negatives = new ArrayList<>();

        if (m == null || m.return1m() == null || m.return3m() == null || m.return6m() == null || m.return12m() == null) {
            return neutralScore("모멘텀 데이터 부족 — 중립 처리");
        }

        // 1. 절대 모멘텀 (50%)
        // 1개월, 3개월, 6개월, 12개월 수익률 평균 계산
        double avgReturn = (m.return1m().doubleValue() +
                            m.return3m().doubleValue() +
                            m.return6m().doubleValue() +
                            m.return12m().doubleValue()) / 4.0;

        // ±10%p 범위를 0~100으로 선형 보간 (평균 0% 일 때 50점)
        double absoluteScoreDouble = (Math.max(-10.0, Math.min(10.0, avgReturn)) + 10.0) * 5.0;
        int absoluteScore = (int) Math.round(absoluteScoreDouble);

        // 시장 국면 보정: 하락장(BEAR) 시 20% 감점, 급락장(CRISIS) 시 50% 감점
        MarketRegime regime = input.marketRegime();
        double regimeMultiplier = 1.0;
        if (regime == MarketRegime.BEAR) {
            regimeMultiplier = 0.8;
            negatives.add("시장 국면 하락장(BEAR) 감점 반영 (-20%)");
        } else if (regime == MarketRegime.CRISIS) {
            regimeMultiplier = 0.5;
            negatives.add("시장 국면 급락장(CRISIS) 감점 반영 (-50%)");
        } else if (regime == MarketRegime.STRONG_BULL) {
            positives.add("시장 국면 강세장(STRONG_BULL) 우호적 환경");
        }

        int finalAbsoluteScore = (int) Math.round(absoluteScore * regimeMultiplier);
        detail.put("absolute_momentum", finalAbsoluteScore);

        // 2. 상대 모멘텀 (50%)
        // RS Rating (0~100) 그대로 사용
        int relativeScore = m.rsRating();
        detail.put("relative_momentum", relativeScore);

        // 3. 최종 점수 (절대 모멘텀 50% + 상대 모멘텀 50%)
        int totalScore = (int) Math.round(finalAbsoluteScore * 0.5 + relativeScore * 0.5);
        totalScore = Math.max(0, Math.min(100, totalScore));

        // 피드백 메시지 구성
        if (avgReturn > 5.0) {
            positives.add(String.format("중장기 절대 모멘텀 우상향 (평균 수익률: %.2f%%)", avgReturn));
        } else if (avgReturn < -3.0) {
            negatives.add(String.format("중장기 절대 모멘텀 하락세 (평균 수익률: %.2f%%)", avgReturn));
        }

        if (relativeScore >= 70) {
            positives.add(String.format("상대적 강세 (RS Rating: %d) — 시장 초과수익 달성 중", relativeScore));
        } else if (relativeScore <= 30) {
            negatives.add(String.format("상대적 약세 (RS Rating: %d) — 시장 대비 언더퍼폼", relativeScore));
        }

        return new StrategyScore(
                StrategyType.DUAL_MOMENTUM,
                totalScore,
                StrategyScore.gradeOf(totalScore),
                Map.copyOf(detail),
                List.copyOf(positives),
                List.copyOf(negatives)
        );
    }

    private StrategyScore neutralScore(String reason) {
        return new StrategyScore(
                StrategyType.DUAL_MOMENTUM,
                NEUTRAL,
                StrategyScore.gradeOf(NEUTRAL),
                Map.of("absolute_momentum", NEUTRAL, "relative_momentum", NEUTRAL),
                List.of(),
                List.of(reason)
        );
    }
}
