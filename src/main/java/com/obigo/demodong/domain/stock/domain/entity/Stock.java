package com.obigo.demodong.domain.stock.domain.entity;

import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String ticker;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketType marketType;

    // ★ 관심 종목 여부 — 모닝 브리핑 배치 대상 구분에 사용
    @Column(nullable = false)
    private boolean isWatchlist;

    public void updateWatchlist(boolean isWatchlist) {
        this.isWatchlist = isWatchlist;
    }
}
