package com.obigo.demodong.domain.fundamental.infrastructure;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DART 재무제표 계정과목 매핑 테이블.
 * XBRL 코드가 회사마다 다르므로, 계정명 패턴(contains 매칭) 방식으로 추출한다.
 *
 * <p>우선순위: 목록 앞쪽 패턴이 더 정확한 명칭. 첫 매칭 시 반환.</p>
 */
@Component
public class AccountCodeMapper {

    /**
     * 필드명 → 계정명 패턴 우선순위 목록.
     * DART /fnlttSinglAcntAll.json 의 account_nm 필드와 매칭.
     */
    private static final Map<String, List<String>> PATTERNS = Map.of(
            "netIncome",          List.of("당기순이익", "당기순손익", "연결당기순이익", "분기순이익"),
            "totalAssets",        List.of("자산총계"),
            "operatingCashFlow",  List.of("영업활동현금흐름", "영업활동으로 인한 현금흐름", "영업활동에서 창출된 현금흐름"),
            "totalLiabilities",   List.of("부채총계"),
            "currentAssets",      List.of("유동자산"),
            "currentLiabilities", List.of("유동부채"),
            "grossProfit",        List.of("매출총이익", "매출총손익"),
            "revenue",            List.of("매출액", "수익(매출액)", "영업수익", "매출"),
            "sharesOutstanding",  List.of("보통주발행주식수", "발행주식수", "보통주식의 발행주식수")
    );

    /**
     * 계정 목록에서 fieldName에 해당하는 금액을 추출한다.
     *
     * @param fieldName  추출할 필드명 (PATTERNS 키와 일치)
     * @param accounts   DART API에서 받은 계정 목록
     * @param useCurrent true = 당기(thstrm_amount), false = 전기(frmtrm_amount)
     * @return 금액 (BigDecimal), 매칭 실패 또는 파싱 실패 시 null
     */
    public BigDecimal findAmount(String fieldName, List<DartAccount> accounts, boolean useCurrent) {
        List<String> patterns = PATTERNS.get(fieldName);
        if (patterns == null || accounts == null) return null;

        for (String pattern : patterns) {
            for (DartAccount account : accounts) {
                if (account.accountNm() != null && account.accountNm().contains(pattern)) {
                    String raw = useCurrent ? account.thstrmAmount() : account.frmtrmAmount();
                    return parseSafely(raw);
                }
            }
        }
        return null;
    }

    /**
     * 발행주식수 전용 Long 추출 (BigDecimal 파싱 후 longValue() 변환).
     */
    public Long findLongAmount(String fieldName, List<DartAccount> accounts, boolean useCurrent) {
        BigDecimal value = findAmount(fieldName, accounts, useCurrent);
        return value != null ? value.longValue() : null;
    }

    private BigDecimal parseSafely(String raw) {
        if (raw == null || raw.isBlank() || "-".equals(raw.trim())) return null;
        try {
            // DART API 금액은 쉼표(,) 포함 가능
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * DART 재무제표 계정 항목 DTO.
     * /fnlttSinglAcntAll.json 응답의 list 항목에 해당.
     */
    public record DartAccount(
            String accountNm,       // 계정명 (account_nm)
            String thstrmAmount,    // 당기 금액 (thstrm_amount)
            String frmtrmAmount     // 전기 금액 (frmtrm_amount)
    ) {}
}
