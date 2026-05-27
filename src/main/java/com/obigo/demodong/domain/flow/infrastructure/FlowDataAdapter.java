package com.obigo.demodong.domain.flow.infrastructure;

import com.obigo.demodong.domain.flow.domain.model.FlowSnapshot;
import com.obigo.demodong.domain.flow.domain.port.FlowDataPort;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link FlowDataPort} 구현체.
 *
 * <ul>
 *   <li>USA → stub() 즉시 반환 (KIS investor API는 KOR 전용)</li>
 *   <li>KOR → {@link KisFlowAdapter} 호출</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class FlowDataAdapter implements FlowDataPort {

    private final KisFlowAdapter kisFlowAdapter;

    @Override
    public FlowSnapshot fetch(String ticker, MarketType market) {
        if (market == MarketType.USA) {
            return FlowSnapshot.stub();
        }
        return kisFlowAdapter.fetchInvestorFlow(ticker);
    }
}
