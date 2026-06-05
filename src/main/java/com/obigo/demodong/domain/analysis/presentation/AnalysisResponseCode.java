package com.obigo.demodong.domain.analysis.presentation;

import com.obigo.demodong.global.common.response.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AnalysisResponseCode implements ResponseCode {

    ANALYSIS_SUCCESS(200, HttpStatus.OK, "종목 분석이 완료되었습니다."),
    STOCK_SEARCH_SUCCESS(200, HttpStatus.OK, "종목 검색이 완료되었습니다."),
    MARKET_REGIME_SUCCESS(200, HttpStatus.OK, "시장 국면 조회가 완료되었습니다."),
    SCREENING_SUCCESS(200, HttpStatus.OK, "종목 스크리닝이 완료되었습니다.");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
