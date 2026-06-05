package com.obigo.demodong.domain.portfolio.presentation;

import com.obigo.demodong.domain.portfolio.application.dto.request.PortfolioRegisterRequest;
import com.obigo.demodong.domain.portfolio.application.dto.request.WatchlistRegisterRequest;
import com.obigo.demodong.domain.portfolio.application.dto.response.PortfolioResponse;
import com.obigo.demodong.domain.portfolio.application.dto.response.WatchListResponse;
import com.obigo.demodong.domain.portfolio.application.usecase.PortFolioUseCase;
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

    // ── 보유 종목 ──────────────────────────────────

    @Operation(summary = "보유 종목 목록")
    @GetMapping("/portfolio")
    public ApiResponse<List<PortfolioResponse>> getPortfolio() {
        return ApiResponse.ok(PortfolioResponseCode.PORTFOLIO_LIST_SUCCESS, portFolioUseCase.getPortfolio());
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

    @Operation(summary = "관심 종목 목록")
    @GetMapping("/watchlist")
    public ApiResponse<List<WatchListResponse>> getWatchlist() {
        return ApiResponse.ok(PortfolioResponseCode.WATCHLIST_LIST_SUCCESS, portFolioUseCase.getWatchlist());
    }

    @Operation(summary = "관심 종목 등록")
    @PostMapping("/watchlist")
    public ApiResponse<WatchListResponse> registerWatchlist(@RequestBody @Valid WatchlistRegisterRequest request) {
        return ApiResponse.ok(PortfolioResponseCode.WATCHLIST_REGISTER_SUCCESS, portFolioUseCase.registerWatchlist(request));
    }

    @Operation(summary = "관심 종목 삭제")
    @DeleteMapping("/watchlist/{stockId}")
    public ApiResponse<Void> deleteWatchlist(@PathVariable Long stockId) {
        portFolioUseCase.deleteWatchlist(stockId);
        return ApiResponse.ok(PortfolioResponseCode.WATCHLIST_DELETE_SUCCESS);
    }
}
