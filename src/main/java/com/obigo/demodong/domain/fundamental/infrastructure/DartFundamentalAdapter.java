package com.obigo.demodong.domain.fundamental.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.obigo.demodong.domain.fundamental.domain.model.FundamentalSnapshot;
import com.obigo.demodong.domain.fundamental.infrastructure.AccountCodeMapper.DartAccount;
import com.obigo.demodong.global.common.infrastructure.dart.DartProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * DART 재무제표 API 어댑터.
 * {@code /fnlttSinglAcntAll.json} 으로 연간 재무제표를 조회하여
 * {@link FundamentalSnapshot}의 재무 분석 필드를 채운다.
 *
 * <p>조회 전략:</p>
 * <ol>
 *   <li>사업연도 = 현재 연도 - 1 (직전 완료된 연간 보고서)</li>
 *   <li>CFS(연결재무제표) 우선 → 실패 시 OFS(별도재무제표) 재시도</li>
 *   <li>API 오류 또는 파싱 실패 → log.warn 후 null 필드만 유지</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DartFundamentalAdapter {

    private final WebClient dartWebClient;
    private final DartProperties dartProperties;
    private final AccountCodeMapper accountCodeMapper;

    private static final String REPORT_CODE_ANNUAL = "11013"; // 사업보고서
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    /**
     * DART 재무제표에서 Piotroski 계산용 재무 데이터를 조회한다.
     *
     * @param dartCorpCode DART 법인코드 (8자리)
     * @return 재무 데이터가 채워진 FundamentalSnapshot (조회 실패 시 필드 null)
     */
    public FundamentalSnapshot fetchFinancials(String dartCorpCode) {
        String bsnsYear = String.valueOf(LocalDate.now().getYear() - 1);

        List<DartAccount> accounts = fetchAccounts(dartCorpCode, bsnsYear, "CFS");
        if (accounts.isEmpty()) {
            log.debug("[DART-Fundamental] CFS 없음, OFS 재시도 - corpCode: {}", dartCorpCode);
            accounts = fetchAccounts(dartCorpCode, bsnsYear, "OFS");
        }
        if (accounts.isEmpty()) {
            log.warn("[DART-Fundamental] 재무제표 조회 실패 - corpCode: {}, year: {}", dartCorpCode, bsnsYear);
            return FundamentalSnapshot.stub();
        }

        return buildSnapshot(accounts, dartCorpCode);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Private Methods
    // ──────────────────────────────────────────────────────────────────────

    private List<DartAccount> fetchAccounts(String corpCode, String bsnsYear, String fsDiv) {
        try {
            DartFinancialResponse response = dartWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/fnlttSinglAcntAll.json")
                            .queryParam("crtfc_key", dartProperties.apiKey())
                            .queryParam("corp_code", corpCode)
                            .queryParam("bsns_year", bsnsYear)
                            .queryParam("reprt_code", REPORT_CODE_ANNUAL)
                            .queryParam("fs_div", fsDiv)
                            .build())
                    .retrieve()
                    .bodyToMono(DartFinancialResponse.class)
                    .block(Duration.ofSeconds(15));

            if (response == null || !"000".equals(response.status()) || response.list() == null) {
                log.debug("[DART-Fundamental] {} 조회 실패 - corpCode: {}, status: {}",
                        fsDiv, corpCode, response != null ? response.status() : "null");
                return Collections.emptyList();
            }
            return response.list().stream()
                    .map(item -> new DartAccount(item.accountNm(), item.thstrmAmount(), item.frmtrmAmount()))
                    .toList();
        } catch (Exception e) {
            log.warn("[DART-Fundamental] {} API 호출 예외 - corpCode: {}, error: {}", fsDiv, corpCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    private FundamentalSnapshot buildSnapshot(List<DartAccount> accounts, String corpCode) {
        try {
            // ── 당기 원시 값 ──
            BigDecimal netIncome        = accountCodeMapper.findAmount("netIncome",          accounts, true);
            BigDecimal totalAssets      = accountCodeMapper.findAmount("totalAssets",        accounts, true);
            BigDecimal operatingCF      = accountCodeMapper.findAmount("operatingCashFlow",  accounts, true);
            BigDecimal totalLiabilities = accountCodeMapper.findAmount("totalLiabilities",   accounts, true);
            BigDecimal currentAssets    = accountCodeMapper.findAmount("currentAssets",      accounts, true);
            BigDecimal currentLiab      = accountCodeMapper.findAmount("currentLiabilities", accounts, true);
            BigDecimal grossProfit      = accountCodeMapper.findAmount("grossProfit",        accounts, true);
            BigDecimal revenue          = accountCodeMapper.findAmount("revenue",            accounts, true);
            Long sharesOut              = accountCodeMapper.findLongAmount("sharesOutstanding", accounts, true);
            BigDecimal operatingProfit  = accountCodeMapper.findAmount("operatingProfit",    accounts, true);

            // ── 전기 원시 값 ──
            BigDecimal prevNetIncome    = accountCodeMapper.findAmount("netIncome",          accounts, false);
            BigDecimal prevTotalAssets  = accountCodeMapper.findAmount("totalAssets",        accounts, false);
            BigDecimal prevTotalLiab    = accountCodeMapper.findAmount("totalLiabilities",   accounts, false);
            BigDecimal prevCurrentAssets= accountCodeMapper.findAmount("currentAssets",      accounts, false);
            BigDecimal prevCurrentLiab  = accountCodeMapper.findAmount("currentLiabilities", accounts, false);
            BigDecimal prevGrossProfit  = accountCodeMapper.findAmount("grossProfit",        accounts, false);
            BigDecimal prevRevenue      = accountCodeMapper.findAmount("revenue",            accounts, false);
            Long prevSharesOut          = accountCodeMapper.findLongAmount("sharesOutstanding", accounts, false);

            // ── 당기 비율 계산 ──
            BigDecimal roa                 = dividePercent(netIncome, totalAssets);
            BigDecimal debtRatio           = dividePercent(totalLiabilities, totalAssets);
            BigDecimal currentRatio        = divide(currentAssets, currentLiab);
            BigDecimal grossProfitMargin   = dividePercent(grossProfit, revenue);
            BigDecimal assetTurnover       = divide(revenue, totalAssets);

            // ── Magic Formula 당기 ROIC 계산 (영업이익 / (총자산 - 유동부채) * 100) ──
            BigDecimal investedCapital = null;
            if (totalAssets != null && currentLiab != null) {
                investedCapital = totalAssets.subtract(currentLiab);
            }
            BigDecimal roic = dividePercent(operatingProfit, investedCapital);

            // ── 전기 비율 계산 ──
            BigDecimal prevRoa             = dividePercent(prevNetIncome, prevTotalAssets);
            BigDecimal prevDebtRatio       = dividePercent(prevTotalLiab, prevTotalAssets);
            BigDecimal prevCurrentRatio    = divide(prevCurrentAssets, prevCurrentLiab);
            BigDecimal prevGPM             = dividePercent(prevGrossProfit, prevRevenue);
            BigDecimal prevAssetTurnover   = divide(prevRevenue, prevTotalAssets);

            // ── 성장률 ──
            BigDecimal revenueGrowthYoy = growthRate(revenue, prevRevenue);

            return new FundamentalSnapshot(
                    null, null, null, null,         // per/pbr/eps/marketCap — KisFundamentalAdapter에서 채움
                    null,                           // roe — KisFundamentalAdapter에서 채움
                    roa, prevRoa,
                    operatingCF, totalAssets,
                    debtRatio, prevDebtRatio,
                    currentRatio, prevCurrentRatio,
                    grossProfitMargin, prevGPM,
                    assetTurnover, prevAssetTurnover,
                    revenueGrowthYoy,
                    sharesOut, prevSharesOut,
                    roic,
                    null,                           // earningsYield — FundamentalDataAdapter.merge에서 채움
                    operatingProfit
            );
        } catch (Exception e) {
            log.warn("[DART-Fundamental] 스냅샷 생성 실패 - corpCode: {}, error: {}", corpCode, e.getMessage());
            return FundamentalSnapshot.stub();
        }
    }

    // ── 계산 헬퍼 ──

    /** numerator / denominator × 100 (%). 분모 0 또는 null → null */
    private BigDecimal dividePercent(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return null;
        return numerator.divide(denominator, MC).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    /** numerator / denominator. 분모 0 또는 null → null */
    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return null;
        return numerator.divide(denominator, MC).setScale(4, RoundingMode.HALF_UP);
    }

    /** (current - prev) / prev × 100 (%). prev 0 또는 null → null */
    private BigDecimal growthRate(BigDecimal current, BigDecimal prev) {
        if (current == null || prev == null || prev.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(prev).divide(prev.abs(), MC)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Response Records (Jackson)
    // ──────────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DartFinancialResponse(
            String status,
            String message,
            List<DartFinancialItem> list
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DartFinancialItem(
            @JsonProperty("account_nm")      String accountNm,
            @JsonProperty("thstrm_amount")   String thstrmAmount,
            @JsonProperty("frmtrm_amount")   String frmtrmAmount
    ) {}
}
