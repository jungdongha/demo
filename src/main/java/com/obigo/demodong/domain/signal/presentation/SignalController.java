package com.obigo.demodong.domain.signal.presentation;

import com.obigo.demodong.domain.signal.application.dto.response.SignalHistoryResponse;
import com.obigo.demodong.domain.signal.application.dto.response.StockAnalysisResponse;
import com.obigo.demodong.domain.signal.application.usecase.StockAnalysisUseCase;
import com.obigo.demodong.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SignalController {

    private final StockAnalysisUseCase stockAnalysisUseCase;

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
        return ApiResponse.ok(SignalResponseCode.REPORT_LIST_SUCCESS, stockAnalysisUseCase.getTodayReports());
    }

    @GetMapping("/reports/{ticker}/history")
    public ApiResponse<Page<SignalHistoryResponse>> getHistory(
            @PathVariable String ticker,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(SignalResponseCode.REPORT_LIST_SUCCESS, stockAnalysisUseCase.getHistoryByTicker(ticker, pageable));
    }
}
