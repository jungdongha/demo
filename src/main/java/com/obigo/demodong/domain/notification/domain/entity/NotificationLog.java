package com.obigo.demodong.domain.notification.domain.entity;

import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 텔레그램 알림 발송 이력 엔티티.
 * 발송 성공/실패 여부와 오류 메시지를 보관한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "notification_log")
@SuperBuilder
public class NotificationLog extends BaseEntity {

    @Column(length = 20)
    private String ticker;          // nullable — 일괄 발송 시 null

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;    // nullable — 성공 시 null

    public static NotificationLog success(String ticker, String message) {
        return NotificationLog.builder()
                .ticker(ticker)
                .message(message)
                .sentAt(LocalDateTime.now())
                .status(NotificationStatus.SENT)
                .build();
    }

    public static NotificationLog failure(String ticker, String message, String errorMessage) {
        return NotificationLog.builder()
                .ticker(ticker)
                .message(message)
                .sentAt(LocalDateTime.now())
                .status(NotificationStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }
}
