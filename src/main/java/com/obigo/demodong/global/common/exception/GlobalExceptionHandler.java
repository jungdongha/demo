package com.obigo.demodong.global.common.exception;

import com.obigo.demodong.global.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
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

    /**
     * @Validated + @Pattern 등 PathVariable/RequestParam 검증 실패 처리.
     * (MethodArgumentNotValidException은 @RequestBody @Valid 실패 처리)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<ErrorResponse>>> handleConstraintViolation(ConstraintViolationException e) {
        ErrorCode errorCode = GlobalErrorCode.INVALID_ARGUMENT;

        List<ErrorResponse> errors = e.getConstraintViolations()
                .stream()
                .map(cv -> {
                    String field = cv.getPropertyPath().toString();
                    // "methodName.paramName" 형태에서 마지막 파라미터명만 추출
                    int dot = field.lastIndexOf('.');
                    String shortField = dot >= 0 ? field.substring(dot + 1) : field;
                    return ErrorResponse.of(shortField, cv.getMessage(), cv.getInvalidValue());
                })
                .collect(Collectors.toList());

        ApiResponse<List<ErrorResponse>> body = ApiResponse.fail(errorCode, errors);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(body);
    }
}
