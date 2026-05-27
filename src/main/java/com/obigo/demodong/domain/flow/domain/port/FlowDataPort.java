package com.obigo.demodong.domain.flow.domain.port;

import com.obigo.demodong.domain.flow.domain.model.FlowSnapshot;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;

/**
 * 투자자별 매매동향 데이터 조회 포트 (DIP).
 *
 * <ul>
 *   <li>KOR → KIS inquire-investor API</li>
 *   <li>USA → {@link FlowSnapshot#stub()} 반환</li>
 * </ul>
 */
public interface FlowDataPort {

    /**
     * 투자자별 매매동향을 조회한다.
     *
     * @param ticker 종목코드
     * @param market 시장 유형 (KOR / USA)
     * @return 수급 스냅샷 (조회 불가 시 stub())
     */
    FlowSnapshot fetch(String ticker, MarketType market);
}
