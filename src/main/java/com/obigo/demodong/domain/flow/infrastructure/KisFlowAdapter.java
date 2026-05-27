package com.obigo.demodong.domain.flow.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.obigo.demodong.domain.flow.domain.model.FlowSnapshot;
import com.obigo.demodong.domain.price.infrastructure.kis.KisTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * KIS 투자자별 매매동향 조회 어댑터 (KOR 전용).
 *
 * <pre>
 * KIS API: GET /uapi/domestic-stock/v1/quotations/inquire-investor
 * tr_id: FHKST01010900 (실전) / VHKST01010900 (모의)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisFlowAdapter {

    private final WebClient kisWebClient;
    private final KisTokenManager kisTokenManager;

    /**
     * 당일 투자자별 순매수 수량을 조회한다.
     *
     * @param ticker KOR 6자리 종목코드
     * @return 수급 스냅샷 (조회 실패 시 stub())
     */
    public FlowSnapshot fetchInvestorFlow(String ticker) {
        try {
            KisInvestorResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-investor")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", ticker)
                            .build())
                    .header("Authorization", "Bearer " + kisTokenManager.getAccessToken())
                    .header("tr_id", kisTokenManager.trId("FHKST01010900", "VHKST01010900"))
                    .retrieve()
                    .bodyToMono(KisInvestorResponse.class)
                    .block();

            if (response == null || response.output() == null) {
                log.debug("[KIS-Flow] 응답 없음 - ticker: {}", ticker);
                return FlowSnapshot.stub();
            }

            KisInvestorOutput o = response.output();
            return new FlowSnapshot(
                    parseLong(o.orgnNtbyQty()),
                    parseLong(o.frgnNtbyQty()),
                    parseLong(o.prsnNtbyQty())
            );
        } catch (Exception e) {
            log.warn("[KIS-Flow] 투자자 매매동향 조회 실패 - ticker: {}, error: {}", ticker, e.getMessage());
            return FlowSnapshot.stub();
        }
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.parseLong(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Response Records (Jackson)
    // ──────────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisInvestorResponse(
            KisInvestorOutput output,
            @JsonProperty("rt_cd") String rtCd,
            @JsonProperty("msg1")  String msg1
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KisInvestorOutput(
            @JsonProperty("orgn_ntby_qty") String orgnNtbyQty,   // 기관 순매수 수량
            @JsonProperty("frgn_ntby_qty") String frgnNtbyQty,   // 외국인 순매수 수량
            @JsonProperty("prsn_ntby_qty") String prsnNtbyQty    // 개인 순매수 수량
    ) {}
}
