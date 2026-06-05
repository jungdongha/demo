package com.obigo.demodong.domain.analysis.application.dto.request;

import java.util.List;

/**
 * 종목 스크리닝 요청 DTO.
 *
 * <p>각 전략별 최소 점수(minScore)를 지정한다. null이면 해당 전략 조건 없음.</p>
 * <p>tickers: 스크리닝 대상 종목 목록. null이면 관심종목 전체 대상.</p>
 */
public record ScreeningRequest(
        Integer canslim,
        Integer momentum,
        Integer seasonality,
        Integer minervini,
        Integer magic,
        Integer piotroski,
        Integer reversion,
        List<String> tickers
) {
    /** 전략 조건이 최소 1개 이상 지정되었는지 검사 */
    public boolean hasAnyCondition() {
        return canslim != null || momentum != null || seasonality != null
                || minervini != null || magic != null
                || piotroski != null || reversion != null;
    }
}
