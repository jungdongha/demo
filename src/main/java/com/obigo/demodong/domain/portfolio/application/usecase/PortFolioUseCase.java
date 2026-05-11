package com.obigo.demodong.domain.portfolio.application.usecase;

import com.obigo.demodong.domain.portfolio.application.dto.request.PortfolioRegisterRequest;
import com.obigo.demodong.domain.portfolio.application.dto.request.WatchlistRegisterRequest;
import com.obigo.demodong.domain.portfolio.application.dto.response.PortfolioResponse;
import com.obigo.demodong.domain.portfolio.application.dto.response.WatchListResponse;
import com.obigo.demodong.domain.portfolio.application.exception.PortfolioErrorCode;
import com.obigo.demodong.domain.portfolio.domain.entity.PortfolioDetail;
import com.obigo.demodong.domain.portfolio.domain.service.PortFolioWriter;
import com.obigo.demodong.domain.portfolio.domain.service.PortfolioReader;
import com.obigo.demodong.domain.price.infrastructure.StockPriceFetcher;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.repository.StockRepository;
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
    private final StockRepository stockRepository;
    private final StockPriceFetcher stockPriceFetcher;

    // -- 보유 종목 --

    public List<PortfolioResponse> getPortfolio() {
        return portfolioReader.findAll().stream()
                .map(detail -> {
                    BigDecimal currentPrice = null;
                    if (detail.getStock().getMarketType() == MarketType.USA) {
                        try {
                            currentPrice = stockPriceFetcher.fetchCurrentPrice(detail.getStock());
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
        return stockRepository.findAllByIsWatchlistTrue().stream()
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
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ApplicationException(PortfolioErrorCode.STOCK_NOT_FOUND));
        stock.updateWatchlist(false);
    }

    // -- private --

    private Stock findOrCreateStock(String ticker) {
        return stockRepository.findByTicker(ticker)
                .orElseGet(() -> {
                    MarketType marketType = ticker.chars()
                            .anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)
                            ? MarketType.KOR : MarketType.USA;
                    return stockRepository.save(
                            Stock.builder()
                                    .ticker(ticker).name(ticker)
                                    .marketType(marketType).isWatchlist(false)
                                    .build()
                    );
                });
    }
}
