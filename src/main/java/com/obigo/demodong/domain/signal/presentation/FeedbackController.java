package com.obigo.demodong.domain.signal.presentation;

import com.obigo.demodong.domain.signal.application.dto.response.FeedbackStatsResponse;
import com.obigo.demodong.domain.signal.application.dto.response.SignalFeedbackResponse;
import com.obigo.demodong.domain.signal.application.usecase.FeedbackStatsUseCase;
import com.obigo.demodong.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Phase 8 — 시그널 피드백 API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackStatsUseCase feedbackStatsUseCase;

    /**
     * 카테고리별 시그널 Alpha 통계 조회
     * GET /api/feedback/stats
     */
    @GetMapping("/stats")
    public ApiResponse<List<FeedbackStatsResponse>> getStats() {
        return ApiResponse.ok(SignalResponseCode.FEEDBACK_STATS_SUCCESS,
                feedbackStatsUseCase.getStatsByCategory());
    }

    /**
     * 최근 피드백 이력 조회 (기본 30건)
     * GET /api/feedback/recent?limit=30
     */
    @GetMapping("/recent")
    public ApiResponse<List<SignalFeedbackResponse>> getRecent(
            @RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.ok(SignalResponseCode.FEEDBACK_LIST_SUCCESS,
                feedbackStatsUseCase.getRecentFeedbacks(limit));
    }

    /**
     * 특정 시그널 피드백 조회
     * GET /api/feedback/{reportId}
     */
    @GetMapping("/{reportId}")
    public ApiResponse<SignalFeedbackResponse> getFeedback(@PathVariable Long reportId) {
        Optional<SignalFeedbackResponse> result = feedbackStatsUseCase.getFeedbackByReportId(reportId);
        if (result.isEmpty()) {
            return ApiResponse.ok(SignalResponseCode.FEEDBACK_NOT_FOUND, null);
        }
        return ApiResponse.ok(SignalResponseCode.FEEDBACK_LIST_SUCCESS, result.get());
    }
}
