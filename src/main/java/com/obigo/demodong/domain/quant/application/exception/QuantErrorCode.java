package com.obigo.demodong.domain.quant.application.exception;

import com.obigo.demodong.global.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum QuantErrorCode implements ErrorCode {

    QUANT_SIGNAL_NOT_FOUND(40501, HttpStatus.NOT_FOUND, "해당 종목의 Quant 시그널을 찾을 수 없습니다."),
    QUANT_UNIVERSE_EMPTY(40502, HttpStatus.SERVICE_UNAVAILABLE, "Quant 유니버스에 분석 대상 종목이 없습니다."),
    MARKET_REGIME_CRISIS(40503, HttpStatus.SERVICE_UNAVAILABLE, "현재 시장 국면이 CRISIS입니다. 시그널 생성을 중단합니다."),
    QUANT_DATA_FETCH_FAILED(40504, HttpStatus.BAD_GATEWAY, "Quant 데이터 수집에 실패했습니다. (KIS/Alpha Vantage)"),
    QUANT_SCORE_CALC_FAILED(40505, HttpStatus.INTERNAL_SERVER_ERROR, "Quant Score 계산 중 오류가 발생했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
