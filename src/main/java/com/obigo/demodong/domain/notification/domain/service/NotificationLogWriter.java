package com.obigo.demodong.domain.notification.domain.service;

import com.obigo.demodong.domain.notification.domain.entity.NotificationLog;
import com.obigo.demodong.domain.notification.domain.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationLogWriter {

    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public void save(NotificationLog log) {
        notificationLogRepository.save(log);
    }
}
