package com.obigo.demodong.domain.portfolio.presentation;

import com.obigo.demodong.domain.portfolio.application.dto.request.PortfolioRegisterRequest;
import com.obigo.demodong.domain.portfolio.application.dto.request.WatchlistRegisterRequest;
import com.obigo.demodong.domain.portfolio.application.dto.response.PortfolioResponse;
import com.obigo.demodong.domain.portfolio.application.dto.response.PortfolioSummaryResponse;
import com.obigo.demodong.domain.portfolio.application.dto.response.WatchListResponse;
import com.obigo.demodong.domain.portfolio.application.dto.response.WatchlistScoreResponse;
import com.obigo.demodong.domain.portfolio.application.usecase.PortFolioUseCase;
import com.obigo.demodong.domain.portfolio.application.usecase.PortfolioSummaryUseCase;
import com.obigo.demodong.domain.portfolio.application.usecase.WatchlistScoreUseCase;
import com.obigo.demodong.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "포트폴리오", description = "보유 종목 및 관심 종목 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PortfolioController {

    private final PortFolioUseCase portFolioUseCase;
    private final PortfolioSummaryUseCase portfolioSummaryUseCase;
    private final WatchlistScoreUseCase watchlistScoreUseCase;

    // ── 보유 종목 ──────────────────────────────────

    @Operation(summary = "보유 종목 목록 조회")
    @GetMapping("/portfolio")
    public ApiResponse<List<PortfolioResponse>> getPortfolio() {
        return ApiResponse.ok(PortfolioResponseCode.PORTFOLIO_LIST_SUCCESS, portFolioUseCase.getPortfolio());
    }

    @Operation(summary = "포트폴리오 요약 조회", description = "보유 종목 전체 수익률 + 전략 점수 평균 + 총 평가금액")
    @GetMapping("/portfolio/summary")
    public ApiResponse<PortfolioSummaryResponse> getPortfolioSummary() {
        return ApiResponse.ok(PortfolioResponseCode.PORTFOLIO_SUMMARY_SUCCESS, portfolioSummaryUseCase.getSummary());
    }

    @Operation(summary = "보유 종목 등록", description = "ticker, quantity, avgPrice 필수")
    @PostMapping("/portfolio")
    public ApiResponse<PortfolioResponse> registerPortfolio(@RequestBody @Valid PortfolioRegisterRequest request) {
        return ApiResponse.ok(PortfolioResponseCode.PORTFOLIO_REGISTER_SUCCESS, portFolioUseCase.registerPortfolio(request));
    }

    @Operation(summary = "보유 종목 삭제 (소프트 딜리트)")
    @DeleteMapping("/portfolio/{id}")
    public ApiResponse<Void> deletePortfolio(@PathVariable Long id) {
        portFolioUseCase.deletePortfolio(id);
        return ApiResponse.ok(PortfolioResponseCode.PORTFOLIO_DELETE_SUCCESS);
    }

    // ── 관심 종목 ──────────────────────────────────

    @Operation(summary = "관심종목 목록 조회")
    @GetMapping("/watchlist")
    public ApiResponse<List<WatchListResponse>> getWatchlist() {
        return ApiResponse.ok(PortfolioResponseCode.WATCHLIST_LIST_SUCCESS, portFolioUseCase.getWatchlist());
    }

    @Operation(summary = "관심종목 전체 점수 조회", description = "현재가 + 7개 전략 점수 병렬 조회, totalScore 내림차순")
    @GetMapping("/watchlist/scores")
    public ApiResponse<List<WatchlistScoreResponse>> getWatchlistScores() {
        return ApiResponse.ok(PortfolioResponseCode.WATCHLIST_SCORES_SUCCESS, watchlistScoreUseCase.getScores());
    }

    @Operation(summary = "관심종목 등록")
    @PostMapping("/watchlist")
    public ApiResponse<WatchListResponse> registerWatchlist(@RequestBody @Valid WatchlistRegisterRequest request) {
        return ApiResponse.ok(PortfolioResponseCode.WATCHLIST_REGISTER_SUCCESS, portFolioUseCase.registerWatchlist(request));
    }

    @Operation(summary = "관심종목 삭제")
    @DeleteMapping("/watchlist/{stockId}")
    public ApiResponse<Void> deleteWatchlist(@PathVariable Long stockId) {
        portFolioUseCase.deleteWatchlist(stockId);
        return ApiResponse.ok(PortfolioResponseCode.WATCHLIST_DELETE_SUCCESS);
    }
}
