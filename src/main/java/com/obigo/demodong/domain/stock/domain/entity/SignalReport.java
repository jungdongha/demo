package com.obigo.demodong.domain.stock.domain.entity;

import com.obigo.demodong.domain.stock.domain.enums.SignalType;
import com.obigo.demodong.domain.stock.domain.enums.SourceType;
import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "signal_report")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignalReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // ★ BUY / HOLD / SELL — AI 응답에서 파싱해서 저장
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignalType signalType;

    // ★ AI 판단 근거 3줄 (응답에서 "판단 근거" 섹션 파싱)
    @Column(columnDefinition = "TEXT")
    private String reason;

    // ★ AI 응답 전문 (마크다운 포맷 그대로 저장)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // ★ 크롤링 원문 보존 — 프롬프트 개선 시 재분석 가능하도록 반드시 저장
    @Column(columnDefinition = "TEXT")
    private String rawNewsText;

    // ★ ON_DEMAND(사용자 요청) / SCHEDULED(배치 자동) 구분
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType sourceType;
}
