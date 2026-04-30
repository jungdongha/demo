package com.obigo.demodong.global.common.response;

import org.springframework.http.HttpStatus;

public interface ResponseCode {
    int getCode();
    HttpStatus getHttpStatus();
    String getMessage();

}
