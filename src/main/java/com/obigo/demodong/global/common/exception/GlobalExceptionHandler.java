package com.obigo.demodong.global.common.exception;

import com.obigo.demodong.global.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;


@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 우리가 만든 ApplicationException이 터지면 일로 옵니다.
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleException(ApplicationException e) {
        ErrorCode errorCode = e.getErrorCode();
        String errorMessage = e.getMessage() != null ? e.getMessage() : errorCode.getMessage();
        // ApiResponse.fail()을 통해 에러 응답 규격도 통일합니다.
        ApiResponse<Void> body = ApiResponse.fail(errorCode, errorMessage);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(body);
    }

    /**
     * 입력값이 잘못되었을 때(Validation 실패) 처리하는 곳입니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ErrorResponse>>> handleValidation(MethodArgumentNotValidException e) {
        ErrorCode errorCode = GlobalErrorCode.INVALID_ARGUMENT;

        List<ErrorResponse> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.of(
                        fe.getField(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "",
                        fe.getRejectedValue()
                ))
                .collect(Collectors.toList());

        ApiResponse<List<ErrorResponse>> body = ApiResponse.fail(errorCode, errors);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(body);
    }

    // ... (이하 생략 - 동일한 방식으로 작동함)
}
