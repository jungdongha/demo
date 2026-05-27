package com.obigo.demodong.domain.fundamental.domain.port;

import com.obigo.demodong.domain.fundamental.domain.model.FundamentalSnapshot;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;

/**
 * 재무 데이터 조회 포트 (OCP 준수).
 *
 * <p>구현 규칙:</p>
 * <ul>
 *   <li>market == USA 또는 dartCorpCode == null → {@link FundamentalSnapshot#stub()} 반환</li>
 *   <li>API 오류 시 log.warn 후 stub() 반환 (예외 전파 금지)</li>
 * </ul>
 */
public interface FundamentalDataPort {

    /**
     * 재무 데이터를 조회한다.
     *
     * @param ticker       종목코드
     * @param dartCorpCode DART 법인코드 (KOR 전용, null 허용)
     * @param market       시장 구분
     * @return 재무 스냅샷 (조회 불가 시 stub())
     */
    FundamentalSnapshot fetch(String ticker, String dartCorpCode, MarketType market);
}
