package com.obigo.demodong.domain.price.domain.port;

import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.stock.domain.entity.Stock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockPricePort {
    BigDecimal fetchCurrentPrice(Stock stock);
    List<PriceSnapshot> fetchMonthlyPrices(Stock stock);
    String formatPriceHistory(List<PriceSnapshot> snapshots);

    /**
     * 52주 고가/저가 조회. 미지원 시 Optional.empty() 반환.
     * @return [고가, 저가] 쌍
     */
    Optional<BigDecimal[]> fetch52WeekRange(Stock stock);
}

