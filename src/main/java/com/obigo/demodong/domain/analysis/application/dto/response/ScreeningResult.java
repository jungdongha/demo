package com.obigo.demodong.domain.analysis.application.dto.response;

import java.util.List;
import java.util.Map;

/**
 * 종목 스크리닝 결과 DTO.
 *
 * <p>스크리닝 조건을 만족하는 종목의 점수 요약과 통과 전략 목록을 담는다.</p>
 */
public record ScreeningResult(
        String ticker,
        String companyName,
        Map<String, Integer> scores,        // StrategyType.name() → 점수
        int totalScore,                     // 7개 전략 점수 평균
        List<String> passedStrategies       // 조건을 통과한 전략 목록
) {}
