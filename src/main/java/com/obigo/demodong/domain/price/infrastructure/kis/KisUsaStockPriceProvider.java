package com.obigo.demodong.domain.price.infrastructure.kis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.price.domain.repository.PriceSnapshotRepository;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisUsaStockPriceProvider {

    private final WebClient kisWebClient;
    private final KisTokenManager kisTokenManager;
    private final PriceSnapshotRepository priceSnapshotRepository;

    // KIS VTS는 해외주식 미지원 — 실전 tr_id만 사용
    private static final String TR_ID_CURRENT = "HHDFS00000300";
    private static final String TR_ID_DAILY   = "HHDFS76240000";
    private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** NASDAQ 우선 시도 후 NYSE 폴백 */
    public BigDecimal fetchCurrentPrice(Stock stock) {
        for (String excd : List.of("NAS", "NYS", "AMS")) {
            try {
                KisUsaCurrentResponse response = kisWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/uapi/overseas-price/v1/quotations/price")
                                .queryParam("AUTH", "")
                                .queryParam("EXCD", excd)
                                .queryParam("SYMB", stock.getTicker())
                                .build())
                        .header("Authorization", "Bearer " + kisTokenManager.getAccessToken())
                        .header("tr_id", TR_ID_CURRENT)
                        .retrieve()
                        .bodyToMono(KisUsaCurrentResponse.class)
                        .block();

                if (response != null && response.output() != null
                        && response.output().last() != null
                        && !response.output().last().isBlank()) {
                    return new BigDecimal(response.output().last());
                }
            } catch (Exception e) {
                log.debug("[KIS-USA] {} 거래소 현재가 조회 실패 - ticker: {}", excd, stock.getTicker());
            }
        }
        log.warn("[KIS-USA] 현재가 조회 실패 - ticker: {}", stock.getTicker());
        return null;
    }

    public List<PriceSnapshot> fetchDailyPrices(Stock stock) {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusMonths(1);

        List<PriceSnapshot> cached = priceSnapshotRepository
                .findByStockAndRecordedDateBetweenOrderByRecordedDateAsc(stock, monthAgo, today);

        boolean hasTodayData = cached.stream().anyMatch(p -> p.getRecordedDate().equals(today));
        if (hasTodayData) {
            log.info("[KIS-USA] 캐시 사용 - ticker: {}", stock.getTicker());
            return cached;
        }

        for (String excd : List.of("NAS", "NYS", "AMS")) {
            try {
                KisUsaDailyResponse response = kisWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/uapi/overseas-price/v1/quotations/dailyprice")
                                .queryParam("AUTH", "")
                                .queryParam("EXCD", excd)
                                .queryParam("SYMB", stock.getTicker())
                                .queryParam("GUBN", "0")
                                .queryParam("BYMD", "")
                                .queryParam("MODP", "1")
                                .build())
                        .header("Authorization", "Bearer " + kisTokenManager.getAccessToken())
                        .header("tr_id", TR_ID_DAILY)
                        .retrieve()
                        .bodyToMono(KisUsaDailyResponse.class)
                        .block();

                if (response == null || response.output2() == null || response.output2().isEmpty()) continue;

                List<PriceSnapshot> fetched = new ArrayList<>();
                for (KisUsaDailyItem item : response.output2()) {
                    if (item.xymd() == null || item.clos() == null || item.clos().isBlank()) continue;
                    LocalDate date = LocalDate.parse(item.xymd(), KIS_DATE);
                    if (priceSnapshotRepository.findByStockAndRecordedDate(stock, date).isPresent()) continue;

                    PriceSnapshot snapshot = priceSnapshotRepository.save(
                            PriceSnapshot.builder()
                                    .stock(stock)
                                    .closePrice(new BigDecimal(item.clos()))
                                    .recordedDate(date)
                                    .build()
                    );
                    fetched.add(snapshot);
                }
                log.info("[KIS-USA] 시세 조회 완료 - ticker: {}, excd: {}, {}건", stock.getTicker(), excd, fetched.size());
                return fetched.isEmpty() ? cached : fetched;
            } catch (Exception e) {
                log.debug("[KIS-USA] {} 거래소 시세 조회 실패 - ticker: {}", excd, stock.getTicker());
            }
        }
        log.warn("[KIS-USA] 시세 조회 실패 (전 거래소) - ticker: {}", stock.getTicker());
        return cached;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisUsaCurrentResponse(KisUsaCurrentOutput output) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisUsaCurrentOutput(@JsonProperty("last") String last) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisUsaDailyResponse(@JsonProperty("output2") List<KisUsaDailyItem> output2) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisUsaDailyItem(
            @JsonProperty("xymd") String xymd,
            @JsonProperty("clos") String clos
    ) {}
}
