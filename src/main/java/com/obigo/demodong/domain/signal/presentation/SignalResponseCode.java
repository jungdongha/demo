package com.obigo.demodong.domain.signal.presentation;

import com.obigo.demodong.global.common.response.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SignalResponseCode implements ResponseCode {

    STOCK_ANALYSIS_SUCCESS(200, HttpStatus.OK, "주식 분석이 완료되었습니다."),
    REPORT_LIST_SUCCESS(200, HttpStatus.OK, "시그널 목록을 조회했습니다."),
    FEEDBACK_STATS_SUCCESS(200, HttpStatus.OK, "시그널 피드백 통계를 조회했습니다."),
    FEEDBACK_LIST_SUCCESS(200, HttpStatus.OK, "시그널 피드백 목록을 조회했습니다."),
    FEEDBACK_NOT_FOUND(200, HttpStatus.OK, "해당 시그널의 피드백이 아직 없습니다.");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
