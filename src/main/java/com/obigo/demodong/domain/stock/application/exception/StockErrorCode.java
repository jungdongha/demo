package com.obigo.demodong.domain.stock.application.exception;

import com.obigo.demodong.global.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StockErrorCode implements ErrorCode {

    STOCK_NOT_FOUND(40301, HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다."),

    /** 한글 입력 시 DART에서 복수 후보가 발견된 경우 — 정확한 이름 입력 필요 */
    STOCK_NAME_AMBIGUOUS(40302, HttpStatus.BAD_REQUEST, "종목명이 모호합니다. 정확한 법인명을 입력해 주세요."),

    /** KOR 종목인데 DART corp_code가 매핑되지 않은 경우 — 공시 조회 불가 */
    DART_CORP_CODE_NOT_MAPPED(40303, HttpStatus.UNPROCESSABLE_ENTITY, "DART 법인코드가 매핑되지 않아 공시 조회가 불가합니다. 티커(숫자 6자리)로 다시 시도해 주세요.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
