package com.obigo.demodong.domain.analysis.presentation;

import com.obigo.demodong.domain.analysis.application.dto.request.ScreeningRequest;
import com.obigo.demodong.domain.analysis.application.dto.response.ScreeningResult;
import com.obigo.demodong.domain.analysis.application.usecase.ScreeningUseCase;
import com.obigo.demodong.global.common.exception.ApplicationException;
import com.obigo.demodong.global.common.exception.GlobalErrorCode;
import com.obigo.demodong.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 종목 스크리닝 REST API 컨트롤러.
 *
 * <p>GET /api/screening — 전략 점수 조건 기반 종목 필터링</p>
 */
@Tag(name = "종목 스크리닝", description = "전략 점수 조건 기반 종목 필터링 (AND 조건). 최소 1개 전략 조건 필요.")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ScreeningController {

    private final ScreeningUseCase screeningUseCase;

    @Operation(
            summary = "전략 점수 기반 종목 스크리닝",
            description = """
                    지정한 전략의 최소 점수를 AND 조건으로 만족하는 종목을 반환한다.
                    - 점수 범위: 0~100
                    - 최소 1개 전략 조건 필수
                    - tickers 미지정 시 관심종목 전체 대상
                    """
    )
    @GetMapping("/screening")
    public ApiResponse<List<ScreeningResult>> screen(
            @Parameter(description = "CAN SLIM 최소 점수 (0~100)")
            @RequestParam(required = false) Integer canslim,
            @Parameter(description = "듀얼 모멘텀 최소 점수 (0~100)")
            @RequestParam(required = false) Integer momentum,
            @Parameter(description = "시즌성 최소 점수 (0~100)")
            @RequestParam(required = false) Integer seasonality,
            @Parameter(description = "미네르비니 최소 점수 (0~100)")
            @RequestParam(required = false) Integer minervini,
            @Parameter(description = "Magic Formula 최소 점수 (0~100)")
            @RequestParam(required = false) Integer magic,
            @Parameter(description = "Piotroski F-Score 최소 점수 (0~100)")
            @RequestParam(required = false) Integer piotroski,
            @Parameter(description = "Mean Reversion 최소 점수 (0~100)")
            @RequestParam(required = false) Integer reversion,
            @Parameter(description = "스크리닝 대상 종목 (쉼표 구분, 예: 005930,AAPL). 미지정 시 관심종목 전체.")
            @RequestParam(required = false) String tickers
    ) {
        validateScoreRanges(canslim, momentum, seasonality, minervini, magic, piotroski, reversion);

        List<String> tickerList = parseTickerList(tickers);

        ScreeningRequest request = new ScreeningRequest(
                canslim, momentum, seasonality, minervini, magic, piotroski, reversion, tickerList
        );

        if (!request.hasAnyCondition()) {
            throw new ApplicationException(GlobalErrorCode.INVALID_ARGUMENT);
        }

        log.info("[API] GET /api/screening — conditions: canslim={}, momentum={}, minervini={}, piotroski={}, magic={}, reversion={}, seasonality={}",
                canslim, momentum, minervini, piotroski, magic, reversion, seasonality);

        return ApiResponse.ok(AnalysisResponseCode.SCREENING_SUCCESS, screeningUseCase.screen(request));
    }

    // ─────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────

    private void validateScoreRanges(Integer... scores) {
        for (Integer score : scores) {
            if (score != null && (score < 0 || score > 100)) {
                throw new ApplicationException(GlobalErrorCode.INVALID_ARGUMENT);
            }
        }
    }

    private List<String> parseTickerList(String tickers) {
        if (tickers == null || tickers.isBlank()) return null;
        return Arrays.stream(tickers.split(","))
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .toList();
    }
}
