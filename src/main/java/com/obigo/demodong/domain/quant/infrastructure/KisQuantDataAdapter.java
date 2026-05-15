package com.obigo.demodong.domain.quant.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.obigo.demodong.domain.price.infrastructure.kis.KisTokenManager;
import com.obigo.demodong.domain.quant.domain.model.DailyQuote;
import com.obigo.demodong.domain.quant.domain.port.QuantDataPort;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * KOR 전용 Quant 데이터 어댑터.
 * KisTokenManager 재활용 (Spring Bean 공유).
 * USA 데이터는 AlphaVantageQuantAdapter(Stub)가 담당하며,
 * 라우팅은 QuantEngineUseCase에서 결정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisQuantDataAdapter implements QuantDataPort {

    private final WebClient kisWebClient;
    private final KisTokenManager kisTokenManager;

    private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** KOR: KIS 일별 시세 조회 / USA: 빈 목록 반환 (Stub) */
    @Override
    public List<DailyQuote> fetchRecentPrices(String ticker, MarketType market, int days) {
        if (market == MarketType.USA) {
            log.debug("[KisQuantData] USA 데이터는 Stub 반환 - ticker={}", ticker);
            return Collections.emptyList();
        }
        return fetchKorDailyPrices(ticker, days);
    }

    /** KOR: KOSPI 지수 5일 변동률 / USA: 0.0 고정 (SIDEWAYS) */
    @Override
    public double fetchIndexReturn5d(MarketType market) {
        if (market == MarketType.USA) {
            log.debug("[KisQuantData] USA 지수 Stub → 0.0 반환");
            return 0.0;
        }
        return fetchKospiReturn5d();
    }

    private List<DailyQuote> fetchKorDailyPrices(String ticker, int days) {
        try {
            KisDailyPriceResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-daily-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", ticker)
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_ORG_ADJ_PRC", "0")
                            .build())
                    .header("Authorization", "Bearer " + kisTokenManager.getAccessToken())
                    .header("tr_id", kisTokenManager.trId("FHKST01010400", "VHKST01010400"))
                    .retrieve()
                    .bodyToMono(KisDailyPriceResponse.class)
                    .block();

            if (response == null || response.output2() == null) return Collections.emptyList();

            List<DailyQuote> result = new ArrayList<>();
            for (KisDailyItem item : response.output2()) {
                if (item.stckBsopDate() == null || item.stckClpr() == null) continue;
                LocalDate date = LocalDate.parse(item.stckBsopDate(), KIS_DATE);
                Long volume = parseVolume(item.acmlVol());
                result.add(new DailyQuote(date, new BigDecimal(item.stckClpr()), volume));
                if (result.size() >= days) break;
            }

            // 최신순 정렬
            result.sort((a, b) -> b.date().compareTo(a.date()));
            log.info("[KisQuantData] KOR 시세 조회 - ticker={}, {}건", ticker, result.size());
            return result;

        } catch (Exception e) {
            log.warn("[KisQuantData] KOR 시세 조회 실패 - ticker={}, error={}", ticker, e.getMessage());
            return Collections.emptyList();
        }
    }

    private double fetchKospiReturn5d() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate from = today.minusDays(10); // 영업일 감안하여 여유있게 조회

            KisIndexResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                            .queryParam("FID_INPUT_ISCD", "0001")  // KOSPI
                            .queryParam("FID_INPUT_DATE_1", from.format(KIS_DATE))
                            .queryParam("FID_INPUT_DATE_2", today.format(KIS_DATE))
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .build())
                    .header("Authorization", "Bearer " + kisTokenManager.getAccessToken())
                    .header("tr_id", "FHKUP03500100")
                    .retrieve()
                    .bodyToMono(KisIndexResponse.class)
                    .block();

            if (response == null || response.output2() == null || response.output2().size() < 2) {
                log.warn("[KisQuantData] KOSPI 지수 데이터 부족 → 0.0 반환");
                return 0.0;
            }

            List<KisIndexItem> items = response.output2();
            // output2는 최신순 정렬 가정
            double latest = parseDouble(items.get(0).bstpNmixPrpr());
            double past5d = parseDouble(items.get(Math.min(items.size() - 1, 4)).bstpNmixPrpr());

            if (past5d == 0.0) return 0.0;
            double ret = (latest - past5d) / past5d * 100.0;
            log.info("[KisQuantData] KOSPI 5일 변동률: {}%", ret);
            return ret;

        } catch (Exception e) {
            log.warn("[KisQuantData] KOSPI 지수 조회 실패 → 0.0 반환: {}", e.getMessage());
            return 0.0;
        }
    }

    private Long parseVolume(String vol) {
        if (vol == null || vol.isBlank()) return null;
        try { return Long.parseLong(vol); } catch (NumberFormatException e) { return null; }
    }

    private double parseDouble(String val) {
        if (val == null || val.isBlank()) return 0.0;
        try { return Double.parseDouble(val.replace(",", "")); } catch (NumberFormatException e) { return 0.0; }
    }

    // ──────────── Response Records ────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisDailyPriceResponse(@JsonProperty("output2") List<KisDailyItem> output2) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisDailyItem(
            @JsonProperty("stck_bsop_date") String stckBsopDate,
            @JsonProperty("stck_clpr")      String stckClpr,
            @JsonProperty("acml_vol")       String acmlVol
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisIndexResponse(@JsonProperty("output2") List<KisIndexItem> output2) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisIndexItem(
            @JsonProperty("stck_bsop_date")   String stckBsopDate,
            @JsonProperty("bstp_nmix_prpr")   String bstpNmixPrpr  // 지수 종가
    ) {}
}
