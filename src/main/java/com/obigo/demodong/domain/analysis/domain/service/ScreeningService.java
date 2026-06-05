package com.obigo.demodong.domain.analysis.domain.service;

import com.obigo.demodong.domain.analysis.application.dto.request.ScreeningRequest;
import com.obigo.demodong.domain.analysis.application.dto.response.AnalysisResponse;
import com.obigo.demodong.domain.analysis.application.dto.response.ScreeningResult;
import com.obigo.demodong.domain.analysis.application.dto.response.StrategyScoreResponse;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 종목 스크리닝 도메인 서비스.
 *
 * <p>전략 점수 조건(AND)을 만족하는 종목을 필터링하고 totalScore 내림차순으로 정렬한다.</p>
 * <p>순수 도메인 로직만 담당 — 외부 의존성 없음.</p>
 */
@Service
public class ScreeningService {

    /**
     * 분석 결과 목록에서 조건을 만족하는 종목만 필터링하여 반환한다.
     *
     * @param analyses 분석 결과 목록
     * @param request  전략별 최소 점수 조건
     * @return 조건 통과 종목 목록 (totalScore 내림차순)
     */
    public List<ScreeningResult> filter(List<AnalysisResponse> analyses, ScreeningRequest request) {
        return analyses.stream()
                .filter(a -> passes(a, request))
                .map(a -> toResult(a, request))
                .sorted(Comparator.comparingInt(ScreeningResult::totalScore).reversed())
                .toList();
    }

    // ─────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────

    private boolean passes(AnalysisResponse a, ScreeningRequest req) {
        Map<String, Integer> scores = extractScores(a);
        return meetsCondition(scores, "CANSLIM",        req.canslim())
            && meetsCondition(scores, "DUAL_MOMENTUM",  req.momentum())
            && meetsCondition(scores, "SEASONALITY",    req.seasonality())
            && meetsCondition(scores, "MINERVINI",      req.minervini())
            && meetsCondition(scores, "MAGIC_FORMULA",  req.magic())
            && meetsCondition(scores, "PIOTROSKI",      req.piotroski())
            && meetsCondition(scores, "MEAN_REVERSION", req.reversion());
    }

    /** 조건이 null이면 무조건 통과, 아니면 minScore 이상인지 확인 */
    private boolean meetsCondition(Map<String, Integer> scores, String strategyKey, Integer minScore) {
        if (minScore == null) return true;
        return scores.getOrDefault(strategyKey, 0) >= minScore;
    }

    private ScreeningResult toResult(AnalysisResponse a, ScreeningRequest req) {
        Map<String, Integer> scores = extractScores(a);
        int totalScore = scores.values().stream()
                .mapToInt(Integer::intValue)
                .sum() / Math.max(scores.size(), 1);

        List<String> passed = collectPassedStrategies(scores, req);

        return new ScreeningResult(a.ticker(), a.name(), scores, totalScore, passed);
    }

    private List<String> collectPassedStrategies(Map<String, Integer> scores, ScreeningRequest req) {
        List<String> passed = new ArrayList<>();
        addIfPassed(passed, scores, "CANSLIM",        req.canslim());
        addIfPassed(passed, scores, "DUAL_MOMENTUM",  req.momentum());
        addIfPassed(passed, scores, "SEASONALITY",    req.seasonality());
        addIfPassed(passed, scores, "MINERVINI",      req.minervini());
        addIfPassed(passed, scores, "MAGIC_FORMULA",  req.magic());
        addIfPassed(passed, scores, "PIOTROSKI",      req.piotroski());
        addIfPassed(passed, scores, "MEAN_REVERSION", req.reversion());
        return Collections.unmodifiableList(passed);
    }

    private void addIfPassed(List<String> passed, Map<String, Integer> scores,
                             String key, Integer minScore) {
        if (minScore != null && scores.getOrDefault(key, 0) >= minScore) {
            passed.add(key);
        }
    }

    private Map<String, Integer> extractScores(AnalysisResponse a) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (StrategyScoreResponse s : a.strategies()) {
            scores.put(s.type(), s.score());
        }
        return Collections.unmodifiableMap(scores);
    }
}
