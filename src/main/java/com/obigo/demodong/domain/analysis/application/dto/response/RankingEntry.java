package com.obigo.demodong.domain.analysis.application.dto.response;

/**
 * 전략별 랭킹 단건 응답.
 *
 * @param rank        순위 (1-based)
 * @param ticker      종목코드
 * @param companyName 회사명
 * @param score       해당 전략 점수 (0~100, null 가능)
 */
public record RankingEntry(int rank, String ticker, String companyName, Integer score) {}
