package com.obigo.demodong.domain.ai.presentation;

import com.obigo.demodong.global.common.response.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiResponseCode implements ResponseCode {

    AI_CHAT_SUCCESS(201, HttpStatus.OK, "AI 응답이 성공적으로 처리되었습니다.");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
