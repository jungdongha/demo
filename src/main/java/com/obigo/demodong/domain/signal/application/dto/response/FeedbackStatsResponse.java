package com.obigo.demodong.domain.signal.application.dto.response;

/**
 * Phase 8 — 카테고리별 시그널 Alpha 통계 응답
 */
public record FeedbackStatsResponse(
        String category,        // 판단 근거 카테고리
        int totalCount,         // 총 시그널 수 (피드백 있는 것)
        int evaluatedCount,     // T+10 평가 완료 수
        int successCount,       // T+10 기준 방향 일치 수
        double successRate,     // 성공률 (%) — 0.0 if evaluatedCount == 0
        double avgAlpha10d,     // T+10 평균 수익률 (%) — 0.0 if none
        int failureCount        // 실패 수 (is_failure = true)
) {
    public static FeedbackStatsResponse of(
            String category,
            int total,
            int evaluated,
            int success,
            double avgAlpha10d,
            int failure) {
        double rate = evaluated > 0 ? Math.round(success * 1000.0 / evaluated) / 10.0 : 0.0;
        return new FeedbackStatsResponse(category, total, evaluated, success, rate, avgAlpha10d, failure);
    }
}
