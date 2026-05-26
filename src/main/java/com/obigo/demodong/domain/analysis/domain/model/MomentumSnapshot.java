package com.obigo.demodong.domain.analysis.domain.model;

import java.math.BigDecimal;

/**
 * 기간별 수익률 + RS Rating을 담는 record.
 */
public record MomentumSnapshot(
        BigDecimal return1m,    // 1개월(20거래일) 수익률 (%)
        BigDecimal return3m,    // 3개월(60거래일) 수익률 (%)
        BigDecimal return6m,    // 6개월(120거래일) 수익률 (%)
        BigDecimal return12m,   // 12개월(240거래일) 수익률 (%)
        int rsRating            // 시장 대비 초과수익률 기반 0~100 정규화
) {}
