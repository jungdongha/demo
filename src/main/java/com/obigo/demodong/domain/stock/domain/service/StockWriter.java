package com.obigo.demodong.domain.stock.domain.service;

import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockWriter {

    private final StockRepository stockRepository;

    public Stock save(Stock stock) {
        return stockRepository.save(stock);
    }
}
