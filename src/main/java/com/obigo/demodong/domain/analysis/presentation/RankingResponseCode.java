package com.obigo.demodong.domain.analysis.presentation;

import com.obigo.demodong.global.common.response.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RankingResponseCode implements ResponseCode {

    RANKING_SUCCESS(200, HttpStatus.OK, "전략별 랭킹 조회가 완료되었습니다.");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
