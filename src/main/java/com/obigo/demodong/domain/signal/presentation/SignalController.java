package com.obigo.demodong.domain.signal.presentation;

import com.obigo.demodong.domain.signal.application.dto.response.StockAnalysisResponse;
import com.obigo.demodong.domain.signal.application.usecase.StockAnalysisUseCase;
import com.obigo.demodong.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock")
public class SignalController {

    private final StockAnalysisUseCase stockAnalysisUseCase;

    @GetMapping("/analyze/{query}")
    public ApiResponse<StockAnalysisResponse> analyze(@PathVariable String query) {
        return ApiResponse.ok(SignalResponseCode.STOCK_ANALYSIS_SUCCESS, stockAnalysisUseCase.execute(query));
    }

    @GetMapping(value = "/analyze/{query}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> analyzeStream(@PathVariable String query) {
        return stockAnalysisUseCase.executeStream(query);
    }
}
