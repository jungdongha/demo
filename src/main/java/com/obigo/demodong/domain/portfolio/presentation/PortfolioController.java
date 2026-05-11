package com.obigo.demodong.domain.portfolio.presentation;

import com.obigo.demodong.domain.portfolio.application.dto.request.PortfolioRegisterRequest;
import com.obigo.demodong.domain.portfolio.application.dto.request.WatchlistRegisterRequest;
import com.obigo.demodong.domain.portfolio.application.dto.response.PortfolioResponse;
import com.obigo.demodong.domain.portfolio.application.dto.response.WatchListResponse;
import com.obigo.demodong.domain.portfolio.application.usecase.PortFolioUseCase;
import com.obigo.demodong.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PortfolioController {

    private final PortFolioUseCase portFolioUseCase;

    // ── 보유 종목 ──────────────────────────────────

    @GetMapping("/portfolio")
    public ApiResponse<List<PortfolioResponse>> getPortfolio() {
        return ApiResponse.ok(PortfolioResponseCode.PORTFOLIO_LIST_SUCCESS, portFolioUseCase.getPortfolio());
    }

    @PostMapping("/portfolio")
    public ApiResponse<PortfolioResponse> registerPortfolio(@RequestBody PortfolioRegisterRequest request) {
        return ApiResponse.ok(PortfolioResponseCode.PORTFOLIO_REGISTER_SUCCESS, portFolioUseCase.registerPortfolio(request));
    }

    @DeleteMapping("/portfolio/{id}")
    public ApiResponse<Void> deletePortfolio(@PathVariable Long id) {
        portFolioUseCase.deletePortfolio(id);
        return ApiResponse.ok(PortfolioResponseCode.PORTFOLIO_DELETE_SUCCESS);
    }

    // ── 관심 종목 ──────────────────────────────────

    @GetMapping("/watchlist")
    public ApiResponse<List<WatchListResponse>> getWatchlist() {
        return ApiResponse.ok(PortfolioResponseCode.WATCHLIST_LIST_SUCCESS, portFolioUseCase.getWatchlist());
    }

    @PostMapping("/watchlist")
    public ApiResponse<WatchListResponse> registerWatchlist(@RequestBody WatchlistRegisterRequest request) {
        return ApiResponse.ok(PortfolioResponseCode.WATCHLIST_REGISTER_SUCCESS, portFolioUseCase.registerWatchlist(request));
    }

    @DeleteMapping("/watchlist/{stockId}")
    public ApiResponse<Void> deleteWatchlist(@PathVariable Long stockId) {
        portFolioUseCase.deleteWatchlist(stockId);
        return ApiResponse.ok(PortfolioResponseCode.WATCHLIST_DELETE_SUCCESS);
    }
}
