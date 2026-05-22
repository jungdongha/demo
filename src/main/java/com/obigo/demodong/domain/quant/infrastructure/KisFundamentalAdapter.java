package com.obigo.demodong.domain.quant.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.obigo.demodong.domain.price.infrastructure.kis.KisTokenManager;
import com.obigo.demodong.domain.quant.domain.model.QuantFundamentalData;
import com.obigo.demodong.domain.quant.domain.port.QuantFundamentalPort;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

/**
 * KOR 전용 재무 펀더멘털 어댑터.
 * KIS /uapi/domestic-stock/v1/quotations/inquire-price 에서 PER·PBR·현재가 조회.
 * USA는 Stub(currentPrice=0, per/pbr/targetPrice=null) 반환 — Phase 10에서 Alpha Vantage 연동 예정.
 * 목표주가(targetPrice)는 Phase 10에서 Naver 스크래핑 추가 예정 — 현재 null.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisFundamentalAdapter implements QuantFundamentalPort {

    private final WebClient kisWebClient;
    private final KisTokenManager kisTokenManager;

    @Override
    public QuantFundamentalData fetchFundamentals(String ticker, MarketType market) {
        if (market == MarketType.USA) {
            log.debug("[KisFundamental] USA Stub 반환 - ticker={}", ticker);
            return QuantFundamentalData.stub(BigDecimal.ZERO);
        }
        return fetchKorFundamentals(ticker);
    }

    private QuantFundamentalData fetchKorFundamentals(String ticker) {
        try {
            KisPriceResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", ticker)
                            .build())
                    .header("Authorization", "Bearer " + kisTokenManager.getAccessToken())
                    .header("tr_id", kisTokenManager.trId("FHKST01010100", "VHKST01010100"))
                    .retrieve()
                    .bodyToMono(KisPriceResponse.class)
                    .block();

            if (response == null || response.output() == null) {
                log.warn("[KisFundamental] KOR 응답 없음 - ticker={}", ticker);
                return QuantFundamentalData.stub(BigDecimal.ZERO);
            }

            KisPriceOutput out = response.output();
            BigDecimal currentPrice = parseBigDecimal(out.stckPrpr());
            BigDecimal per = parseBigDecimalNullable(out.per());
            BigDecimal pbr = parseBigDecimalNullable(out.pbr());

            log.info("[KisFundamental] KOR - ticker={}, price={}, per={}, pbr={}", ticker, currentPrice, per, pbr);
            // targetPrice = null (Phase 10 Naver 스크래핑 예정)
            return new QuantFundamentalData(per, pbr, null, currentPrice);

        } catch (Exception e) {
            log.warn("[KisFundamental] KOR 조회 실패 - ticker={}, error={}", ticker, e.getMessage());
            return QuantFundamentalData.stub(BigDecimal.ZERO);
        }
    }

    private BigDecimal parseBigDecimal(String val) {
        if (val == null || val.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.replace(",", ""));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /** 0 또는 빈 문자열이면 null 반환 (적자 기업 PER 등 의미없는 0 구분) */
    private BigDecimal parseBigDecimalNullable(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            BigDecimal parsed = new BigDecimal(val.replace(",", ""));
            return parsed.compareTo(BigDecimal.ZERO) <= 0 ? null : parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ──────────── Response Records ────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisPriceResponse(@JsonProperty("output") KisPriceOutput output) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisPriceOutput(
            @JsonProperty("stck_prpr") String stckPrpr,  // 주식 현재가
            @JsonProperty("per") String per,              // 주가수익비율
            @JsonProperty("pbr") String pbr               // 주가순자산비율
    ) {
    }
}
