package com.obigo.demodong.domain.stock.domain.service;

import com.obigo.demodong.domain.stock.application.exception.StockErrorCode;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.repository.StockRepository;
import com.obigo.demodong.global.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockReader {

    private final StockRepository stockRepository;

    public Optional<Stock> findByTicker(String ticker) {
        return stockRepository.findByTicker(ticker);
    }

    public Stock findById(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(StockErrorCode.STOCK_NOT_FOUND));
    }

    public List<Stock> findAllByIsWatchlistTrue() {
        return stockRepository.findAllByIsWatchlistTrue();
    }
}
