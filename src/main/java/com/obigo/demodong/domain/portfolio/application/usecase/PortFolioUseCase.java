package com.obigo.demodong.domain.portfolio.application.usecase;

import com.obigo.demodong.domain.portfolio.application.dto.request.PortfolioRegisterRequest;
import com.obigo.demodong.domain.portfolio.application.dto.request.WatchlistRegisterRequest;
import com.obigo.demodong.domain.portfolio.application.dto.response.PortfolioResponse;
import com.obigo.demodong.domain.portfolio.application.dto.response.WatchListResponse;
import com.obigo.demodong.domain.portfolio.domain.entity.PortfolioDetail;
import com.obigo.demodong.domain.portfolio.domain.service.PortFolioWriter;
import com.obigo.demodong.domain.portfolio.domain.service.PortfolioReader;
import com.obigo.demodong.domain.price.domain.port.StockPricePort;
import com.obigo.demodong.domain.price.infrastructure.dart.DartCorpCodeMapper;
import com.obigo.demodong.domain.stock.application.exception.StockErrorCode;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import com.obigo.demodong.domain.stock.domain.service.StockWriter;
import com.obigo.demodong.global.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortFolioUseCase {

    private final PortfolioReader portfolioReader;
    private final PortFolioWriter portFolioWriter;
    private final StockReader stockReader;
    private final StockWriter stockWriter;
    private final StockPricePort stockPricePort;
    private final DartCorpCodeMapper dartCorpCodeMapper;

    // -- 보유 종목 --

    public List<PortfolioResponse> getPortfolio() {
        return portfolioReader.findAll().stream()
                .map(detail -> {
                    BigDecimal currentPrice = null;
                    if (detail.getStock().getMarketType() == MarketType.USA) {
                        try {
                            currentPrice = stockPricePort.fetchCurrentPrice(detail.getStock());
                        } catch (Exception e) {
                            log.warn("현재가 조회 실패 - ticker: {}", detail.getStock().getTicker());
                        }
                    }
                    return PortfolioResponse.from(detail, currentPrice);
                })
                .toList();
    }

    @Transactional
    public PortfolioResponse registerPortfolio(PortfolioRegisterRequest request) {
        Stock stock = findOrCreateStock(request.ticker());
        PortfolioDetail detail = portFolioWriter.save(
                PortfolioDetail.builder()
                        .stock(stock)
                        .quantity(request.quantity())
                        .avgPrice(request.avgPrice())
                        .build()
        );
        return PortfolioResponse.from(detail, null);
    }

    @Transactional
    public void deletePortfolio(Long id) {
        PortfolioDetail detail = portfolioReader.findById(id);
        detail.softDelete();
    }

    // -- 관심 종목 --

    public List<WatchListResponse> getWatchlist() {
        return stockReader.findAllByIsWatchlistTrue().stream()
                .map(WatchListResponse::from)
                .toList();
    }

    @Transactional
    public WatchListResponse registerWatchlist(WatchlistRegisterRequest request) {
        Stock stock = findOrCreateStock(request.ticker());
        stock.updateWatchlist(true);
        return WatchListResponse.from(stock);
    }

    @Transactional
    public void deleteWatchlist(Long stockId) {
        Stock stock = stockReader.findById(stockId);
        stock.updateWatchlist(false);
    }

    // -- private --

    private Stock findOrCreateStock(String input) {
        String ticker = resolveTicker(input);
        MarketType marketType = ticker.matches("\\d{6}") ? MarketType.KOR : MarketType.USA;
        return stockReader.findByTicker(ticker)
                .orElseGet(() -> stockWriter.save(
                        Stock.builder()
                                .ticker(ticker)
                                .name(input)
                                .marketType(marketType)
                                .isWatchlist(false)
                                .build()
                ));
    }

    /**
     * 입력값을 실제 티커 코드로 변환.
     * - 6자리 숫자 → KOR 티커
     * - 영문 → USA 티커 (대문자)
     * - 한글 → DART 회사명 검색 → 미발견 시 STOCK_NOT_FOUND
     */
    private String resolveTicker(String input) {
        String trimmed = input.trim();
        if (trimmed.matches("\\d{6}")) return trimmed;
        if (trimmed.matches("[A-Za-z.\\-]{1,10}")) return trimmed.toUpperCase();
        if (trimmed.matches(".*[가-힣].*")) {
            return dartCorpCodeMapper.resolveTickerByName(trimmed)
                    .orElseThrow(() -> {
                        log.warn("종목 검색 실패 - input: {}", trimmed);
                        return new ApplicationException(StockErrorCode.STOCK_NOT_FOUND);
                    });
        }
        throw new ApplicationException(StockErrorCode.STOCK_NOT_FOUND);
    }
}
