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
import java.time.Duration;
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
                    .block(Duration.ofSeconds(10));

            if (response == null || response.output() == null) {
                log.warn("[KIS-KOR] 현재가 응답 없음 - ticker: {}, rt_cd: {}, msg: {}",
                        stock.getTicker(),
                        response != null ? response.rtCd() : "null",
                        response != null ? response.msg1() : "null");
                return null;
            }
            return new BigDecimal(response.output().stckPrpr());
        } catch (Exception e) {
            log.warn("[KIS-KOR] 현재가 조회 실패 - ticker: {}, error: {}", stock.getTicker(), e.getMessage());
            return null;
        }
    }

    public String fetchStockName(Stock stock) {
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
                    .block(Duration.ofSeconds(10));
            if (response == null || response.output() == null) return null;
            return response.output().htsKorIsnm();
        } catch (Exception e) {
            log.warn("[KIS-KOR] 종목명 조회 실패 - ticker: {}", stock.getTicker());
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
                    .block(Duration.ofSeconds(10));

            if (response == null || response.output() == null) {
                log.warn("[KIS-KOR] 52주 응답 없음 - ticker: {}, rt_cd: {}, msg: {}",
                        stock.getTicker(),
                        response != null ? response.rtCd() : "null",
                        response != null ? response.msg1() : "null");
                return Optional.empty();
            }
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

    /** EMA200 계산을 위한 최소 데이터 수 */
    private static final int MIN_PRICE_COUNT = 200;
    /** DB 조회 범위 (거래일 200일 ≈ 약 300 캘린더 일) */
    private static final int PRICE_HISTORY_DAYS = 300;
    /** KIS API 1회 호출당 최대 반환 행 수 */
    private static final int KIS_DAILY_PAGE_SIZE = 30;

    public List<PriceSnapshot> fetchDailyPrices(Stock stock) {
        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusDays(PRICE_HISTORY_DAYS);

        // 1. DB에서 300일치 조회
        List<PriceSnapshot> cached = priceSnapshotRepository
                .findByStockAndRecordedDateBetweenOrderByRecordedDateAsc(stock, from, today);

        // 2. 오늘 데이터가 있고 200건 이상이면 캐시 반환
        boolean hasTodayData = cached.stream().anyMatch(p -> p.getRecordedDate().equals(today));
        if (hasTodayData && cached.size() >= MIN_PRICE_COUNT) {
            log.info("[KIS-KOR] 캐시 사용 - ticker: {}, {}건", stock.getTicker(), cached.size());
            return cached;
        }

        // 3. 부족하면 30일씩 최대 10번 API 호출하여 300일치 적재
        LocalDate endDate = today;
        for (int i = 0; i < 10 && !endDate.isBefore(from); i++) {
            LocalDate startDate = endDate.minusDays(KIS_DAILY_PAGE_SIZE - 1);
            if (startDate.isBefore(from)) startDate = from;
            fetchAndSaveRange(stock, startDate, endDate);
            endDate = startDate.minusDays(1);
        }

        List<PriceSnapshot> result = priceSnapshotRepository
                .findByStockAndRecordedDateBetweenOrderByRecordedDateAsc(stock, from, today);
        log.info("[KIS-KOR] 시세 조회 완료 - ticker: {}, {}건", stock.getTicker(), result.size());
        return result;
    }

    private void fetchAndSaveRange(Stock stock, LocalDate startDate, LocalDate endDate) {
        try {
            final String start = startDate.format(KIS_DATE);
            final String end   = endDate.format(KIS_DATE);

            KisDailyPriceResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-daily-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stock.getTicker())
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_ORG_ADJ_PRC", "0")
                            .queryParam("FID_INPUT_DATE_1", start)
                            .queryParam("FID_INPUT_DATE_2", end)
                            .build())
                    .header("Authorization", "Bearer " + kisTokenManager.getAccessToken())
                    .header("tr_id", kisTokenManager.trId("FHKST01010400", "VHKST01010400"))
                    .retrieve()
                    .bodyToMono(KisDailyPriceResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || response.output() == null) return;

            for (KisDailyItem item : response.output()) {
                if (item.stckBsopDate() == null || item.stckClpr() == null || item.stckClpr().isBlank()) continue;
                LocalDate date = LocalDate.parse(item.stckBsopDate(), KIS_DATE);
                if (priceSnapshotRepository.findByStockAndRecordedDate(stock, date).isPresent()) continue;

                Long volume = null;
                if (item.acmlVol() != null && !item.acmlVol().isBlank()) {
                    try { volume = Long.parseLong(item.acmlVol()); } catch (NumberFormatException ignored) {}
                }
                priceSnapshotRepository.save(
                        PriceSnapshot.builder()
                                .stock(stock)
                                .closePrice(new BigDecimal(item.stckClpr()))
                                .volume(volume)
                                .recordedDate(date)
                                .build()
                );
            }
        } catch (Exception e) {
            log.warn("[KIS-KOR] 기간 시세 조회 실패 - ticker: {}, {}-{}, error: {}",
                    stock.getTicker(), startDate, endDate, e.getMessage());
        }
    }

    // ──────────── Response Records ────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisCurrentPriceResponse(
            KisCurrentOutput output,
            @JsonProperty("rt_cd") String rtCd,
            @JsonProperty("msg1")  String msg1
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisCurrentOutput(
            @JsonProperty("stck_prpr")    String stckPrpr,
            @JsonProperty("hts_kor_isnm") String htsKorIsnm  // 한글 종목명
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Kis52WeekResponse(
            Kis52WeekOutput output,
            @JsonProperty("rt_cd") String rtCd,
            @JsonProperty("msg1")  String msg1
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Kis52WeekOutput(
            @JsonProperty("w52_hgpr") String w52Hgpr,
            @JsonProperty("w52_lwpr") String w52Lwpr
    ) {}

    /** 52주 고가/저가 VO */
    public record Kis52WeekRange(BigDecimal high52, BigDecimal low52) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisDailyPriceResponse(
            @JsonProperty("output")  List<KisDailyItem> output,
            @JsonProperty("rt_cd")   String rtCd,
            @JsonProperty("msg1")    String msg1
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisDailyItem(
            @JsonProperty("stck_bsop_date") String stckBsopDate,
            @JsonProperty("stck_clpr")      String stckClpr,
            @JsonProperty("acml_vol")       String acmlVol       // 누적 거래량
    ) {}
}
