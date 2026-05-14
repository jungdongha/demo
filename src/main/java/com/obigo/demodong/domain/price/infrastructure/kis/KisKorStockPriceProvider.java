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
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisKorStockPriceProvider {

    private final WebClient kisWebClient;
    private final KisTokenManager kisTokenManager;
    private final PriceSnapshotRepository priceSnapshotRepository;

    private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public BigDecimal fetchCurrentPrice(Stock stock) {
        try {
            KisCurrentPriceResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stock.getTicker())
                            .build())
                    .header("Authorization", "Bearer " + kisTokenManager.getAccessToken())
                    .header("tr_id", kisTokenManager.trId("FHKST01010100", "VHKST01010100"))
                    .retrieve()
                    .bodyToMono(KisCurrentPriceResponse.class)
                    .block();

            if (response == null || response.output() == null) return null;
            return new BigDecimal(response.output().stckPrpr());
        } catch (Exception e) {
            log.warn("[KIS-KOR] 현재가 조회 실패 - ticker: {}, error: {}", stock.getTicker(), e.getMessage());
            return null;
        }
    }

    /**
     * 52주 고가/저가를 KIS 현재가 조회 API 출력(output)에서 추출.
     * stck_hgpr = 52주 최고가, stck_lwpr = 52주 최저가
     * 실패 시 Optional.empty() 반환.
     */
    public Optional<Kis52WeekRange> fetch52WeekRange(Stock stock) {
        try {
            Kis52WeekResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stock.getTicker())
                            .build())
                    .header("Authorization", "Bearer " + kisTokenManager.getAccessToken())
                    .header("tr_id", kisTokenManager.trId("FHKST01010100", "VHKST01010100"))
                    .retrieve()
                    .bodyToMono(Kis52WeekResponse.class)
                    .block();

            if (response == null || response.output() == null) return Optional.empty();
            Kis52WeekOutput o = response.output();
            if (o.w52Hgpr() == null || o.w52Lwpr() == null
                    || o.w52Hgpr().isBlank() || o.w52Lwpr().isBlank()) return Optional.empty();

            return Optional.of(new Kis52WeekRange(
                    new BigDecimal(o.w52Hgpr()),
                    new BigDecimal(o.w52Lwpr())
            ));
        } catch (Exception e) {
            log.warn("[KIS-KOR] 52주 고/저가 조회 실패 - ticker: {}, error: {}", stock.getTicker(), e.getMessage());
            return Optional.empty();
        }
    }

    public List<PriceSnapshot> fetchDailyPrices(Stock stock) {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusMonths(1);

        List<PriceSnapshot> cached = priceSnapshotRepository
                .findByStockAndRecordedDateBetweenOrderByRecordedDateAsc(stock, monthAgo, today);

        boolean hasTodayData = cached.stream().anyMatch(p -> p.getRecordedDate().equals(today));
        if (hasTodayData) {
            log.info("[KIS-KOR] 캐시 사용 - ticker: {}", stock.getTicker());
            return cached;
        }

        try {
            KisDailyPriceResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-daily-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stock.getTicker())
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_ORG_ADJ_PRC", "0")
                            .build())
                    .header("Authorization", "Bearer " + kisTokenManager.getAccessToken())
                    .header("tr_id", kisTokenManager.trId("FHKST01010400", "VHKST01010400"))
                    .retrieve()
                    .bodyToMono(KisDailyPriceResponse.class)
                    .block();

            if (response == null || response.output2() == null) return cached;

            List<PriceSnapshot> fetched = new ArrayList<>();
            for (KisDailyItem item : response.output2()) {
                if (item.stckBsopDate() == null || item.stckClpr() == null) continue;
                LocalDate date = LocalDate.parse(item.stckBsopDate(), KIS_DATE);
                if (priceSnapshotRepository.findByStockAndRecordedDate(stock, date).isPresent()) continue;

                Long volume = null;
                if (item.acmlVol() != null && !item.acmlVol().isBlank()) {
                    try { volume = Long.parseLong(item.acmlVol()); } catch (NumberFormatException ignored) {}
                }

                PriceSnapshot snapshot = priceSnapshotRepository.save(
                        PriceSnapshot.builder()
                                .stock(stock)
                                .closePrice(new BigDecimal(item.stckClpr()))
                                .volume(volume)
                                .recordedDate(date)
                                .build()
                );
                fetched.add(snapshot);
            }
            log.info("[KIS-KOR] 시세 조회 완료 - ticker: {}, {}건", stock.getTicker(), fetched.size());
            return fetched.isEmpty() ? cached : fetched;
        } catch (Exception e) {
            log.warn("[KIS-KOR] 시세 조회 실패 - ticker: {}, error: {}", stock.getTicker(), e.getMessage());
            return cached;
        }
    }

    // ──────────── Response Records ────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisCurrentPriceResponse(KisCurrentOutput output) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisCurrentOutput(@JsonProperty("stck_prpr") String stckPrpr) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Kis52WeekResponse(Kis52WeekOutput output) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Kis52WeekOutput(
            @JsonProperty("w52_hgpr") String w52Hgpr,
            @JsonProperty("w52_lwpr") String w52Lwpr
    ) {}

    /** 52주 고가/저가 VO */
    public record Kis52WeekRange(BigDecimal high52, BigDecimal low52) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisDailyPriceResponse(@JsonProperty("output2") List<KisDailyItem> output2) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisDailyItem(
            @JsonProperty("stck_bsop_date") String stckBsopDate,
            @JsonProperty("stck_clpr")      String stckClpr,
            @JsonProperty("acml_vol")       String acmlVol       // 누적 거래량
    ) {}
}
