package com.obigo.demodong.domain.stock.domain.entity;


import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SuperBuilder
@Table(name = "news_report")
public class NewsReport extends BaseEntity {

    // ★ 연관관계 핵심 개념: FetchType.LAZY (지연 로딩)
    //   EAGER(즉시)는 NewsReport를 조회할 때 항상 Stock도 같이 SELECT한다.
    //   LAZY(지연)는 stock 필드에 실제로 접근할 때만 Stock을 조회한다.
    //   실무에서는 성능 문제 때문에 @ManyToOne도 LAZY를 명시적으로 지정하는 것이 표준.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false) // FK 컬럼명 명시
    private Stock stock;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
}
