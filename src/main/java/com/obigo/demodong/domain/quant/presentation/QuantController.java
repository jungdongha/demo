package com.obigo.demodong.domain.quant.presentation;

import com.obigo.demodong.domain.quant.application.dto.response.*;
import com.obigo.demodong.domain.quant.application.usecase.QuantSignalUseCase;
import com.obigo.demodong.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quant")
@RequiredArgsConstructor
public class QuantController {

    private final QuantSignalUseCase quantSignalUseCase;

    /** 오늘의 TOP3 Quant 시그널 조회 (QuantFeatureSnapshot 포함) */
    @GetMapping("/top-signals")
    public ApiResponse<List<QuantSignalResponse>> getTopSignals() {
        List<QuantSignalResponse> signals = quantSignalUseCase.getTopSignals();
        if (signals.isEmpty()) {
            return ApiResponse.ok(QuantResponseCode.QUANT_NO_SIGNAL_TODAY, signals);
        }
        return ApiResponse.ok(QuantResponseCode.QUANT_TOP_SIGNALS_SUCCESS, signals);
    }

    /** 전체 유니버스 최신 Quant Score 목록 */
    @GetMapping("/scores")
    public ApiResponse<List<QuantScoreResponse>> getScores() {
        return ApiResponse.ok(QuantResponseCode.QUANT_SCORES_SUCCESS, quantSignalUseCase.getScores());
    }

    /** 특정 종목 Quant Score 상세 */
    @GetMapping("/scores/{ticker}")
    public ApiResponse<QuantScoreDetailResponse> getScoreDetail(@PathVariable String ticker) {
        return ApiResponse.ok(QuantResponseCode.QUANT_SCORE_DETAIL_SUCCESS,
                quantSignalUseCase.getScoreDetail(ticker));
    }

    /** 현재 분석 유니버스 목록 */
    @GetMapping("/universe")
    public ApiResponse<List<QuantUniverseResponse>> getUniverse() {
        return ApiResponse.ok(QuantResponseCode.QUANT_UNIVERSE_SUCCESS, quantSignalUseCase.getUniverse());
    }

    /** 현재 시장 국면 조회 */
    @GetMapping("/regime")
    public ApiResponse<MarketRegimeResponse> getMarketRegime() {
        return ApiResponse.ok(QuantResponseCode.QUANT_REGIME_SUCCESS, quantSignalUseCase.getMarketRegime());
    }
}
