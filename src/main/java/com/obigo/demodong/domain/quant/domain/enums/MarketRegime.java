package com.obigo.demodong.domain.quant.domain.enums;

public enum MarketRegime {
    STRONG_BULL,   // 강세장 (지수 +3% 이상)
    BULL,          // 상승장
    SIDEWAYS,      // 횡보
    BEAR,          // 하락장
    CRISIS         // 폭락장 — Quant 시그널 생성 중단
}
