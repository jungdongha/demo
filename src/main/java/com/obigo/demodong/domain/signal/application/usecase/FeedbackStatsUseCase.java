package com.obigo.demodong.domain.signal.application.usecase;

import com.obigo.demodong.domain.signal.application.dto.response.FeedbackStatsResponse;
import com.obigo.demodong.domain.signal.application.dto.response.SignalFeedbackResponse;
import com.obigo.demodong.domain.signal.domain.entity.SignalFeedback;
import com.obigo.demodong.domain.signal.domain.service.SignalFeedbackReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 8 — 피드백 통계 조회 유스케이스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackStatsUseCase {

    private final SignalFeedbackReader signalFeedbackReader;

    /**
     * 카테고리별 시그널 Alpha 통계를 반환한다.
     * 평가 완료(T+10 데이터 존재)된 피드백만 통계에 포함된다.
     */
    public List<FeedbackStatsResponse> getStatsByCategory() {
        List<SignalFeedback> all = signalFeedbackReader.findAllWithReport();

        // 카테고리별 그룹핑
        Map<String, List<SignalFeedback>> byCategory = all.stream()
                .collect(Collectors.groupingBy(sf -> {
                    String cat = sf.getReport().getExpectedReasonCategory();
                    return cat != null ? cat : "미분류";
                }));

        return byCategory.entrySet().stream()
                .map(entry -> buildStats(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(FeedbackStatsResponse::successRate).reversed())
                .toList();
    }

    /**
     * 최근 N개 피드백 이력을 반환한다.
     */
    public List<SignalFeedbackResponse> getRecentFeedbacks(int limit) {
        return signalFeedbackReader.findRecentWithReport(limit).stream()
                .map(SignalFeedbackResponse::from)
                .toList();
    }

    /**
     * 특정 시그널의 피드백을 반환한다.
     */
    public Optional<SignalFeedbackResponse> getFeedbackByReportId(Long reportId) {
        return signalFeedbackReader.findAllWithReport().stream()
                .filter(sf -> sf.getReport().getId().equals(reportId))
                .findFirst()
                .map(SignalFeedbackResponse::from);
    }

    // ── private ────────────────────────────────────────────────────────────────

    private FeedbackStatsResponse buildStats(String category, List<SignalFeedback> feedbacks) {
        int total = feedbacks.size();

        List<SignalFeedback> evaluated = feedbacks.stream()
                .filter(sf -> sf.getPriceAfter10d() != null)
                .toList();

        int successCount = (int) evaluated.stream()
                .filter(sf -> Boolean.TRUE.equals(sf.getWasCorrect10d()))
                .count();

        int failureCount = (int) evaluated.stream()
                .filter(sf -> Boolean.TRUE.equals(sf.getIsFailure()))
                .count();

        double avgAlpha10d = evaluated.stream()
                .filter(sf -> sf.getAlpha10d() != null)
                .mapToDouble(sf -> sf.getAlpha10d().doubleValue())
                .average()
                .orElse(0.0);

        // 소수점 2자리로 반올림
        avgAlpha10d = Math.round(avgAlpha10d * 100.0) / 100.0;

        return FeedbackStatsResponse.of(category, total, evaluated.size(), successCount, avgAlpha10d, failureCount);
    }
}
