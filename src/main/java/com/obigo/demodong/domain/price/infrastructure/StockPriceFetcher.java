package com.obigo.demodong.domain.price.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.price.domain.port.StockPricePort;
import com.obigo.demodong.domain.price.domain.repository.PriceSnapshotRepository;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceFetcher implements StockPricePort {

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final ObjectMapper objectMapper;

    private static final String YAHOO_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/";

    // 현재가 반환 (1개월 데이터 중 가장 최신)
    public BigDecimal fetchCurrentPrice(Stock stock) {
        List<PriceSnapshot> snapshots = fetchMonthlyPrices(stock);
        if (snapshots.isEmpty()) return null;
        return snapshots.get(snapshots.size() - 1).getClosePrice();
    }

    // 1개월 주가 흐름 반환
    public List<PriceSnapshot> fetchMonthlyPrices(Stock stock) {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusMonths(1);

        // 오늘 데이터가 이미 있으면 DB에서 반환 (캐싱)
        List<PriceSnapshot> cached = priceSnapshotRepository
                .findByStockAndRecordedDateBetweenOrderByRecordedDateAsc(stock, monthAgo, today);

        boolean hasTodayData = cached.stream()
                .anyMatch(p -> p.getRecordedDate().equals(today));

        if (hasTodayData) {
            log.info("[PriceFetcher] 캐시 사용 - ticker: {}", stock.getTicker());
            return cached;
        }

        // Yahoo Finance 호출
        try {
            String json = WebClient.create(YAHOO_URL)
                    .get()
                    .uri(stock.getTicker() + "?interval=1d&range=1mo")
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<PriceSnapshot> fetched = parseAndSave(stock, json);
            log.info("[PriceFetcher] 완료 - ticker: {}, {}건", stock.getTicker(), fetched.size());
            return fetched;

        } catch (Exception e) {
            log.warn("[PriceFetcher] 실패 - ticker: {}, error: {}", stock.getTicker(), e.getMessage());
            return cached; // 실패 시 캐시 반환
        }
    }

    // AI 프롬프트용 문자열 변환
    public String formatPriceHistory(List<PriceSnapshot> snapshots) {
        if (snapshots.isEmpty()) return "주가 데이터 없음";
        return snapshots.stream()
                .map(s -> s.getRecordedDate() + ": " + s.getClosePrice())
                .collect(Collectors.joining(", "));
    }

    /** Yahoo Finance 기반 구현 — 52주 데이터 미지원 */
    @Override
    public Optional<BigDecimal[]> fetch52WeekRange(Stock stock) {
        return Optional.empty();
    }

    private List<PriceSnapshot> parseAndSave(Stock stock, String json) throws Exception {
        JsonNode result = objectMapper.readTree(json)
                .path("chart").path("result").get(0);

        JsonNode timestamps = result.path("timestamp");
        JsonNode closes = result.path("indicators")
                .path("quote").get(0).path("close");

        List<PriceSnapshot> snapshots = new ArrayList<>();

        for (int i = 0; i < timestamps.size(); i++) {
            if (closes.get(i) == null || closes.get(i).isNull()) continue;

            LocalDate date = Instant.ofEpochSecond(timestamps.get(i).asLong())
                    .atZone(ZoneId.of("America/New_York"))
                    .toLocalDate();

            BigDecimal price = BigDecimal.valueOf(closes.get(i).asDouble())
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            // 이미 저장된 날짜 건너뜀
            if (priceSnapshotRepository.findByStockAndRecordedDate(stock, date).isPresent()) continue;

            PriceSnapshot snapshot = priceSnapshotRepository.save(
                    PriceSnapshot.builder()
                            .stock(stock)
                            .closePrice(price)
                            .recordedDate(date)
                            .build()
            );
            snapshots.add(snapshot);
        }
        return snapshots;
    }
}
