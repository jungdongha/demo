package com.obigo.demodong.domain.stock.application.exception;

import com.obigo.demodong.global.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StockErrorCode implements ErrorCode {

    STOCK_NOT_FOUND(40301, HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
