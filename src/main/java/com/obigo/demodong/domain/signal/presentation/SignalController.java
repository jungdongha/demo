package com.obigo.demodong.domain.signal.presentation;

import com.obigo.demodong.domain.signal.application.dto.response.SignalHistoryResponse;
import com.obigo.demodong.domain.signal.application.dto.response.StockAnalysisResponse;
import com.obigo.demodong.domain.signal.application.usecase.StockAnalysisUseCase;
import com.obigo.demodong.domain.signal.domain.enums.SourceType;
import com.obigo.demodong.domain.signal.domain.repository.SignalReportRepository;
import com.obigo.demodong.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SignalController {

    private final StockAnalysisUseCase stockAnalysisUseCase;
    private final SignalReportRepository signalReportRepository;

    @GetMapping("/stock/analyze/{query}")
    public ApiResponse<StockAnalysisResponse> analyze(@PathVariable String query) {
        return ApiResponse.ok(SignalResponseCode.STOCK_ANALYSIS_SUCCESS, stockAnalysisUseCase.execute(query));
    }

    @GetMapping(value = "/stock/analyze/{query}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> analyzeStream(@PathVariable String query) {
        return stockAnalysisUseCase.executeStream(query);
    }

    @GetMapping("/reports/today")
    public ApiResponse<List<SignalHistoryResponse>> getTodayReports() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<SignalHistoryResponse> result = signalReportRepository
                .findBySourceTypeAndCreatedAtBetweenOrderByCreatedAtDesc(SourceType.SCHEDULED, start, end)
                .stream()
                .map(SignalHistoryResponse::from)
                .toList();
        return ApiResponse.ok(SignalResponseCode.REPORT_LIST_SUCCESS, result);
    }

    @GetMapping("/reports/{stockId}/history")
    public ApiResponse<List<SignalHistoryResponse>> getHistory(@PathVariable Long stockId) {
        List<SignalHistoryResponse> result = signalReportRepository
                .findByStock_IdOrderByCreatedAtDesc(stockId)
                .stream()
                .map(SignalHistoryResponse::from)
                .toList();
        return ApiResponse.ok(SignalResponseCode.REPORT_LIST_SUCCESS, result);
    }
}
