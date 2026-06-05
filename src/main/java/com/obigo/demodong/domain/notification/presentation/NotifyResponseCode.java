package com.obigo.demodong.domain.notification.presentation;

import com.obigo.demodong.global.common.response.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotifyResponseCode implements ResponseCode {

    NOTIFY_SUCCESS(200, HttpStatus.OK, "알림 발송이 완료되었습니다.");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
