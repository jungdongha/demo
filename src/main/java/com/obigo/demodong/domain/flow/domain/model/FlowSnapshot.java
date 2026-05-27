package com.obigo.demodong.domain.flow.domain.model;

/**
 * 투자자별 매매동향 스냅샷.
 *
 * <p>KIS inquire-investor API 기준 당일 순매수 데이터.</p>
 * <p>USA 또는 데이터 없음 → {@link #stub()} 반환.</p>
 *
 * @param institutionalNetBuy 기관 순매수 수량 (주), null = 데이터 미지원
 * @param foreignerNetBuy     외국인 순매수 수량 (주), null = 데이터 미지원
 * @param retailNetBuy        개인 순매수 수량 (주), null = 데이터 미지원
 */
public record FlowSnapshot(
        Long institutionalNetBuy,
        Long foreignerNetBuy,
        Long retailNetBuy
) {

    /** USA 또는 데이터 수집 불가 시 반환하는 stub 인스턴스. */
    public static FlowSnapshot stub() {
        return new FlowSnapshot(null, null, null);
    }

    /**
     * 핵심 필드가 모두 null이면 stub으로 간주한다.
     * CAN SLIM Calculator는 stub 시 S/I 항목 50점 중립으로 처리한다.
     */
    public boolean isStub() {
        return institutionalNetBuy == null && foreignerNetBuy == null;
    }
}
