package com.obigo.demodong.domain.signal.application.dto.response;

import com.obigo.demodong.domain.signal.domain.entity.SignalFeedback;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phase 8 — 단건 시그널 피드백 응답
 */
public record SignalFeedbackResponse(
        Long reportId,
        String ticker,
        String signalType,
        String reasonCategory,
        BigDecimal priceAtSignal,
        BigDecimal priceAfter3d,
        BigDecimal priceAfter10d,
        BigDecimal priceAfter20d,
        BigDecimal alpha3d,
        BigDecimal alpha10d,
        BigDecimal alpha20d,
        BigDecimal mddPct,
        Boolean wasCorrect3d,
        Boolean wasCorrect10d,
        Boolean isFailure,
        LocalDateTime evaluatedAt
) {
    public static SignalFeedbackResponse from(SignalFeedback sf) {
        return new SignalFeedbackResponse(
                sf.getReport().getId(),
                sf.getReport().getStock().getTicker(),
                sf.getReport().getSignalType().name(),
                sf.getReport().getExpectedReasonCategory(),
                sf.getPriceAtSignal(),
                sf.getPriceAfter3d(),
                sf.getPriceAfter10d(),
                sf.getPriceAfter20d(),
                sf.getAlpha3d(),
                sf.getAlpha10d(),
                sf.getAlpha20d(),
                sf.getMddPct(),
                sf.getWasCorrect3d(),
                sf.getWasCorrect10d(),
                sf.getIsFailure(),
                sf.getEvaluatedAt()
        );
    }
}
