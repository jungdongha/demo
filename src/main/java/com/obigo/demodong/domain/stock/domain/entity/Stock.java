package com.obigo.demodong.domain.stock.domain.entity;

import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Stock extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String ticker;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketType marketType;

    // ★ 관심 종목 여부 — 모닝 브리핑 배치 대상 구분에 사용
    @Column(nullable = false)
    private boolean isWatchlist;

    // Phase 5 — 섹터 (반도체, 소비재 등). nullable.
    @Column(length = 50)
    private String sector;

    // Phase 5 — DART 법인코드 (KOR 종목만 해당). nullable.
    @Column(length = 8)
    private String dartCorpCode;

    public void updateWatchlist(boolean isWatchlist) {
        this.isWatchlist = isWatchlist;
    }

    public void updateDartCorpCode(String dartCorpCode) {
        this.dartCorpCode = dartCorpCode;
    }
}
