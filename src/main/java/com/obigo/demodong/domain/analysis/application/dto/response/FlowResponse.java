package com.obigo.demodong.domain.analysis.application.dto.response;

import com.obigo.demodong.domain.flow.domain.model.FlowSnapshot;

/**
 * 수급 분석 응답 DTO.
 * KOR 전용 지표. {@code supported = false}이면 프론트에서 미지원 안내를 렌더링한다.
 */
public record FlowResponse(
        boolean supported,
        Long institutionalNetBuy,   // 기관 순매수 수량 (주)
        Long foreignerNetBuy,       // 외국인 순매수 수량 (주)
        Long retailNetBuy           // 개인 순매수 수량 (주)
) {
    public static FlowResponse from(FlowSnapshot f) {
        if (f == null || f.isStub()) {
            return unsupported();
        }
        return new FlowResponse(
                true,
                f.institutionalNetBuy(),
                f.foreignerNetBuy(),
                f.retailNetBuy()
        );
    }

    /** 미국 주식 또는 수급 데이터 미지원 종목용 */
    public static FlowResponse unsupported() {
        return new FlowResponse(false, null, null, null);
    }
}
