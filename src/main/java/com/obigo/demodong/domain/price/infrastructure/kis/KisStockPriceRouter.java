package com.obigo.demodong.domain.price.infrastructure.kis;

import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.price.domain.port.StockPricePort;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * StockPricePort 라우터 — MarketType에 따라 KOR/USA Provider로 위임.
 * @Primary 로 등록되어 StockPricePort 주입 시 우선 선택됨.
 */
@Primary
@Component
@RequiredArgsConstructor
public class KisStockPriceRouter implements StockPricePort {

    private final KisKorStockPriceProvider korProvider;
    private final KisUsaStockPriceProvider usaProvider;

    @Override
    public BigDecimal fetchCurrentPrice(Stock stock) {
        return stock.getMarketType() == MarketType.KOR
                ? korProvider.fetchCurrentPrice(stock)
                : usaProvider.fetchCurrentPrice(stock);
    }

    @Override
    public List<PriceSnapshot> fetchMonthlyPrices(Stock stock) {
        return stock.getMarketType() == MarketType.KOR
                ? korProvider.fetchDailyPrices(stock)
                : usaProvider.fetchDailyPrices(stock);
    }

    @Override
    public String formatPriceHistory(List<PriceSnapshot> snapshots) {
        if (snapshots.isEmpty()) return "주가 데이터 없음";
        return snapshots.stream()
                .map(s -> s.getRecordedDate() + ": " + s.getClosePrice())
                .collect(Collectors.joining(", "));
    }

    /**
     * 52주 고/저가 조회.
     * KOR: KIS inquire-price output 필드 활용
     * USA: KIS 해외주식 API 미제공 → Optional.empty()
     */
    @Override
    public Optional<BigDecimal[]> fetch52WeekRange(Stock stock) {
        if (stock.getMarketType() != MarketType.KOR) return Optional.empty();
        return korProvider.fetch52WeekRange(stock)
                .map(r -> new BigDecimal[]{r.high52(), r.low52()});
    }
}

