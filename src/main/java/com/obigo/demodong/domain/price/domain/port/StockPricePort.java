package com.obigo.demodong.domain.price.domain.port;

import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.stock.domain.entity.Stock;

import java.math.BigDecimal;
import java.util.List;

public interface StockPricePort {
    BigDecimal fetchCurrentPrice(Stock stock);
    List<PriceSnapshot> fetchMonthlyPrices(Stock stock);
    String formatPriceHistory(List<PriceSnapshot> snapshots);
}
