package com.obigo.demodong.global.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GlobalResponseCode implements ResponseCode {

    SUCCESS(20000, HttpStatus.OK, "요청이 성공적으로 처리되었습니다."),
    CREATED(20001, HttpStatus.CREATED, "리소스가 성공적으로 생성되었습니다.");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
