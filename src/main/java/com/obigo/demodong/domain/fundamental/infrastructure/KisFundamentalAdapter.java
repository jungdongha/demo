package com.obigo.demodong.domain.fundamental.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.obigo.demodong.domain.price.infrastructure.kis.KisTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * KIS inquire-price 에서 PER / PBR / EPS / 시가총액을 조회하는 어댑터.
 * KOR 종목 전용. USA는 null 반환.
 *
 * <p>재사용: 기존 {@code kisWebClient} Bean + {@link KisTokenManager}</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisFundamentalAdapter {

    private final WebClient kisWebClient;
    private final KisTokenManager kisTokenManager;

    /**
     * KIS API에서 시장 밸류에이션 지표를 조회한다.
     *
     * @param ticker 종목코드 (KOR 6자리)
     * @return 지표 묶음 (조회 실패 시 모든 필드 null)
     */
    public KisMarketRatios fetchMarketRatios(String ticker) {
        try {
            KisInquirePriceResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", ticker)
                            .build())
                    .header("Authorization", "Bearer " + kisTokenManager.getAccessToken())
                    .header("tr_id", kisTokenManager.trId("FHKST01010100", "VHKST01010100"))
                    .retrieve()
                    .bodyToMono(KisInquirePriceResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || response.output() == null) {
                log.debug("[KIS-Fundamental] 응답 없음 - ticker: {}", ticker);
                return KisMarketRatios.empty();
            }

            KisInquireOutput o = response.output();
            return new KisMarketRatios(
                    parseDecimal(o.per()),
                    parseDecimal(o.pbr()),
                    parseDecimal(o.eps()),
                    parseMarketCap(o.htsAvls())  // 억 원 단위
            );
        } catch (Exception e) {
            log.warn("[KIS-Fundamental] 시장 지표 조회 실패 - ticker: {}, error: {}", ticker, e.getMessage());
            return KisMarketRatios.empty();
        }
    }

    private BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank() || "0".equals(raw.trim())) return null;
        try {
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** KIS hts_avls 는 억 원 단위 문자열 */
    private BigDecimal parseMarketCap(String raw) {
        return parseDecimal(raw);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  VO
    // ──────────────────────────────────────────────────────────────────────

    public record KisMarketRatios(
            BigDecimal per,
            BigDecimal pbr,
            BigDecimal eps,
            BigDecimal marketCap
    ) {
        public static KisMarketRatios empty() {
            return new KisMarketRatios(null, null, null, null);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Response Records (Jackson)
    // ──────────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisInquirePriceResponse(
            KisInquireOutput output,
            @JsonProperty("rt_cd") String rtCd,
            @JsonProperty("msg1")  String msg1
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisInquireOutput(
            @JsonProperty("per")      String per,
            @JsonProperty("pbr")      String pbr,
            @JsonProperty("eps")      String eps,
            @JsonProperty("hts_avls") String htsAvls   // 시가총액 (억 원)
    ) {}
}
