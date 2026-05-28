package com.obigo.demodong.domain.analysis.application.dto.response;

import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;

import java.util.List;
import java.util.Map;

/**
 * 전략 점수 응답 DTO.
 * {@link StrategyScore}를 API 응답 형태로 변환.
 */
public record StrategyScoreResponse(
        String type,
        String displayName,
        int score,
        String grade,
        Map<String, Integer> detail,
        List<String> positives,
        List<String> negatives
) {
    public static StrategyScoreResponse from(StrategyScore score) {
        return new StrategyScoreResponse(
                score.type().name(),
                score.type().getDisplayName(),
                score.score(),
                score.grade(),
                score.detail(),
                score.positives(),
                score.negatives()
        );
    }

    /** 전략 계산 실패 시 중립 응답 */
    public static StrategyScoreResponse neutral(StrategyType type) {
        return new StrategyScoreResponse(
                type.name(),
                type.getDisplayName(),
                50,
                "B",
                Map.of(),
                List.of(),
                List.of("데이터 부족으로 분석 불가")
        );
    }
}
