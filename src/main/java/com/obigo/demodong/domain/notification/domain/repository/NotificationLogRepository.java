package com.obigo.demodong.domain.notification.domain.repository;

import com.obigo.demodong.domain.notification.domain.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
}
