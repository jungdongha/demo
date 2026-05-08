package com.obigo.demodong.domain.stock.presentation;

import com.obigo.demodong.global.common.response.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StockResponseCode implements ResponseCode {

    STOCK_ANALYSIS_SUCCESS(200, HttpStatus.OK, "주식 분석이 완료되었습니다.");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
