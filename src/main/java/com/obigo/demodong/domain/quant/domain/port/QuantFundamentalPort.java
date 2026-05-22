package com.obigo.demodong.domain.quant.domain.port;

import com.obigo.demodong.domain.quant.domain.model.QuantFundamentalData;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;

/**
 * 재무 펀더멘털 + 컨센서스 데이터 조회 포트 (DIP).
 * KOR: KisFundamentalAdapter / USA: Stub(currentPrice=0)
 */
public interface QuantFundamentalPort {
    QuantFundamentalData fetchFundamentals(String ticker, MarketType market);
}
