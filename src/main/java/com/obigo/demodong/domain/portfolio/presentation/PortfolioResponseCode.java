package com.obigo.demodong.domain.portfolio.presentation;

import com.obigo.demodong.global.common.response.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PortfolioResponseCode implements ResponseCode {

    PORTFOLIO_REGISTER_SUCCESS(201, HttpStatus.CREATED, "보유 종목이 등록되었습니다."),
    PORTFOLIO_DELETE_SUCCESS(200, HttpStatus.OK, "보유 종목이 삭제되었습니다."),
    PORTFOLIO_LIST_SUCCESS(200, HttpStatus.OK, "보유 종목 목록을 조회했습니다."),
    WATCHLIST_REGISTER_SUCCESS(201, HttpStatus.CREATED, "관심 종목이 등록되었습니다."),
    WATCHLIST_DELETE_SUCCESS(200, HttpStatus.OK, "관심 종목이 삭제되었습니다."),
    WATCHLIST_LIST_SUCCESS(200, HttpStatus.OK, "관심 종목 목록을 조회했습니다.");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
