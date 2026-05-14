package com.obigo.demodong.domain.signal.application.dto.response;

import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.enums.SignalType;
import com.obigo.demodong.domain.signal.domain.enums.SourceType;

import java.time.LocalDateTime;

public record SignalHistoryResponse(
        Long reportId,
        String ticker,
        String stockName,
        SignalType signalType,
        String reason,
        SourceType sourceType,
        LocalDateTime createdAt
) {
    public static SignalHistoryResponse from(SignalReport report) {
        return new SignalHistoryResponse(
                report.getId(),
                report.getStock().getTicker(),
                report.getStock().getName(),
                report.getSignalType(),
                report.getReason(),
                report.getSourceType(),
                report.getCreatedAt()
        );
    }
}
