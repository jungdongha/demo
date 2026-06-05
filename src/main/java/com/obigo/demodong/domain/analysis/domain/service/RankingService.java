package com.obigo.demodong.domain.analysis.domain.service;

import com.obigo.demodong.domain.analysis.domain.entity.AnalysisHistory;
import com.obigo.demodong.global.common.exception.ApplicationException;
import com.obigo.demodong.global.common.exception.GlobalErrorCode;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 전략별 랭킹 정렬 서비스.
 * AnalysisHistory 리스트를 받아 특정 전략 점수 기준 내림차순 정렬 후 반환.
 */
@Service
public class RankingService {

    private static final Set<String> VALID_STRATEGIES = Set.of(
            "canslim", "momentum", "seasonality", "minervini",
            "magic", "piotroski", "reversion", "total"
    );

    public void validateStrategy(String strategy) {
        if (!VALID_STRATEGIES.contains(strategy.toLowerCase())) {
            throw new ApplicationException(GlobalErrorCode.INVALID_ARGUMENT);
        }
    }

    /**
     * 전략 점수 기준 내림차순 정렬 — 점수가 null인 종목은 뒤로 밀림.
     */
    public List<AnalysisHistory> sort(List<AnalysisHistory> records, String strategy) {
        return records.stream()
                .sorted(Comparator.comparing(
                        h -> extractScore(h, strategy),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    public Integer extractScore(AnalysisHistory h, String strategy) {
        return switch (strategy.toLowerCase()) {
            case "canslim"     -> h.getCanslimScore();
            case "momentum"    -> h.getMomentumScore();
            case "seasonality" -> h.getSeasonalityScore();
            case "minervini"   -> h.getMinerviniScore();
            case "magic"       -> h.getMagicFormulaScore();
            case "piotroski"   -> h.getPiotroskiScore();
            case "reversion"   -> h.getMeanReversionScore();
            case "total"       -> h.getTotalScore();
            default            -> null;
        };
    }
}
