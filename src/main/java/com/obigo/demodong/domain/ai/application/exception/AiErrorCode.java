package com.obigo.demodong.domain.ai.application.exception;

import com.obigo.demodong.global.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiErrorCode implements ErrorCode {

    AI_SERVICE_UNAVAILABLE(50001, HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스를 현재 이용할 수 없습니다."),
    AI_RESPONSE_EMPTY(50002, HttpStatus.INTERNAL_SERVER_ERROR, "AI로부터 비어있는 응답을 받았습니다."),
    AI_INVALID_REQUEST(50003, HttpStatus.BAD_REQUEST, "AI 요청 인자가 올바르지 않습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
