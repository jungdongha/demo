package com.obigo.demodong.domain.analysis.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 종목 통합 분석 응답 DTO.
 *
 * <p>7개 전략 점수, 기술적/재무/수급 지표를 한 번의 API 호출로 모두 반환한다.</p>
 */
public record AnalysisResponse(
        String ticker,
        String name,
        String market,              // "KOR" or "USA"
        String sector,              // nullable
        LocalDateTime analyzedAt,

        // 7개 전략 점수
        List<StrategyScoreResponse> strategies,

        // 분석 3축
        TechnicalResponse technical,
        FundamentalResponse fundamental,
        FlowResponse flow,

        // 시장 국면
        String marketRegime         // "STRONG_BULL" / "BULL" / "SIDEWAYS" / "BEAR" / "CRISIS"
) {}
