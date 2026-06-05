package com.obigo.demodong.domain.analysis.application.dto.response;

import com.obigo.demodong.domain.analysis.domain.entity.AnalysisHistory;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 분석 히스토리 조회 응답 DTO.
 * 차트 렌더링을 위해 analyzeDate 오름차순 반환 (UseCase에서 정렬).
 */
public record AnalysisHistoryResponse(
        LocalDate analyzeDate,
        Integer totalScore,
        Map<String, Integer> scores    // StrategyType.name() → 점수
) {
    public static AnalysisHistoryResponse from(AnalysisHistory h) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        putIfNotNull(scores, "CANSLIM",        h.getCanslimScore());
        putIfNotNull(scores, "DUAL_MOMENTUM",  h.getMomentumScore());
        putIfNotNull(scores, "SEASONALITY",    h.getSeasonalityScore());
        putIfNotNull(scores, "MINERVINI",      h.getMinerviniScore());
        putIfNotNull(scores, "MAGIC_FORMULA",  h.getMagicFormulaScore());
        putIfNotNull(scores, "PIOTROSKI",      h.getPiotroskiScore());
        putIfNotNull(scores, "MEAN_REVERSION", h.getMeanReversionScore());
        return new AnalysisHistoryResponse(h.getAnalyzeDate(), h.getTotalScore(), scores);
    }

    private static void putIfNotNull(Map<String, Integer> map, String key, Integer value) {
        if (value != null) map.put(key, value);
    }
}
