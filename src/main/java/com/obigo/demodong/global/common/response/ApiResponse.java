package com.obigo.demodong.global.common.response;


import com.obigo.demodong.global.common.exception.ErrorCode;

public record ApiResponse<T>(int code, String message, T data) {

    /**
     * 성공 응답 (반환할 데이터가 없는 경우)
     */
    public static <T> ApiResponse<T> ok(ResponseCode res) {
        return new ApiResponse<>(res.getCode(), res.getMessage(), null);
    }

    /**
     * 성공 응답 (반환할 데이터가 있는 경우)
     */
    public static <T> ApiResponse<T> ok(ResponseCode res, T data) {
        return new ApiResponse<>(res.getCode(), res.getMessage(), data);
    }

    /**
     * 에러 응답 (ErrorCode 인터페이스를 구현한 모든 Enum 수용 가능)
     */
    public static <T> ApiResponse<T> fail(ErrorCode err) {
        return new ApiResponse<>(err.getCode(), err.getMessage(), null);
    }

    /**
     * 에러 응답 (상황에 따라 상세한 에러 메시지를 직접 전달하고 싶을 때 사용)
     */
    public static <T> ApiResponse<T> fail(ErrorCode err, String customMessage) {
        return new ApiResponse<>(err.getCode(), customMessage, null);
    }

    /**
     * 에러 응답 (에러와 함께 추가적인 정보를 전달해야 할 때 사용)
     */
    public static <T> ApiResponse<T> fail(ErrorCode err, T data) {
        return new ApiResponse<>(err.getCode(), err.getMessage(), data);
    }
}