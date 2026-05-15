package com.obigo.demodong.domain.quant.presentation;

import com.obigo.demodong.global.common.response.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum QuantResponseCode implements ResponseCode {

    QUANT_TOP_SIGNALS_SUCCESS(200, HttpStatus.OK, "오늘의 TOP3 Quant 시그널을 조회했습니다."),
    QUANT_SCORES_SUCCESS(200, HttpStatus.OK, "Quant Score 목록을 조회했습니다."),
    QUANT_SCORE_DETAIL_SUCCESS(200, HttpStatus.OK, "종목 Quant Score 상세를 조회했습니다."),
    QUANT_UNIVERSE_SUCCESS(200, HttpStatus.OK, "Quant 유니버스 목록을 조회했습니다."),
    QUANT_REGIME_SUCCESS(200, HttpStatus.OK, "현재 시장 국면을 조회했습니다."),
    QUANT_NO_SIGNAL_TODAY(200, HttpStatus.OK, "오늘 생성된 Quant 시그널이 없습니다.");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
