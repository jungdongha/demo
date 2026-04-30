package com.obigo.demodong.global.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    int getCode();

    HttpStatus getStatus();

    String getMessage();

}
