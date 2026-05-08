package com.obigo.demodong.domain.stock.presentation;

import com.obigo.demodong.domain.stock.application.dto.response.StockAnalysisResponse;
import com.obigo.demodong.domain.stock.application.usecase.StockAnalysisUseCase;
import com.obigo.demodong.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock")
public class StockController {

    private final StockAnalysisUseCase stockAnalysisUseCase;

    // GET /api/stock/analyze/삼성전자  → KOR
    // GET /api/stock/analyze/AAPL     → USA
    @GetMapping("/analyze/{query}")
    public ApiResponse<StockAnalysisResponse> analyze(@PathVariable String query) {
        return ApiResponse.ok(
                StockResponseCode.STOCK_ANALYSIS_SUCCESS,
                stockAnalysisUseCase.execute(query)
        );
    }
}
