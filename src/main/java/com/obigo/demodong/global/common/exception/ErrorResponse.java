package com.obigo.demodong.global.common.exception;

public record ErrorResponse(String errorField,String errorMessage, Object inputValue) {

    public static ErrorResponse of(String field, String message, Object value) {
        return new ErrorResponse(field, message, value);
    }
}
