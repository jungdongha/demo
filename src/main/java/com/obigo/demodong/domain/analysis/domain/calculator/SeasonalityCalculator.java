package com.obigo.demodong.domain.analysis.domain.calculator;

import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.analysis.infrastructure.SeasonalityTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 계절성(Seasonality) 전략 Calculator.
 * 업종별 월별 통계적 계절성을 기반으로 성과 기대치를 점수화한다.
 */
@Component
@RequiredArgsConstructor
public class SeasonalityCalculator implements StrategyCalculator {

    private final SeasonalityTable seasonalityTable;

    @Override
    public StrategyType getSupportedStrategy() {
        return StrategyType.SEASONALITY;
    }

    @Override
    public StrategyScore calculate(StrategyInput input) {
        String sector = input.sector();
        Month currentMonth = getCurrentMonth();

        int score = seasonalityTable.getScore(sector, currentMonth);

        Map<String, Integer> detail = new HashMap<>();
        detail.put("seasonality_score", score);

        List<String> positives = new ArrayList<>();
        List<String> negatives = new ArrayList<>();

        String displaySector = (sector != null && !sector.isBlank()) ? sector : "시장 전체";

        if (score >= 70) {
            positives.add(String.format("%s 업종 %s월 계절적 강세 진입 (계절성 점수: %d)", displaySector, currentMonth.getValue(), score));
        } else if (score <= 45) {
            negatives.add(String.format("%s 업종 %s월 계절적 약세 진입 (계절성 점수: %d)", displaySector, currentMonth.getValue(), score));
        }

        return new StrategyScore(
                StrategyType.SEASONALITY,
                score,
                StrategyScore.gradeOf(score),
                Map.copyOf(detail),
                List.copyOf(positives),
                List.copyOf(negatives)
        );
    }

    /**
     * 현재 평가 시점의 월을 반환한다. (재정의 가능하도록 protected 선언)
     */
    protected Month getCurrentMonth() {
        return LocalDate.now().getMonth();
    }
}
