package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.NotificationOutboxEntity;
import com.example.monitoring.common.domain.NotificationStatus;
import com.example.monitoring.common.repo.NotificationOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationOutboxService {

    private static final int PAGE_SIZE = 100;

    private final NotificationOutboxRepository repository;

    public NotificationOutboxService(NotificationOutboxRepository repository) {
        this.repository = repository;
    }

    public List<NotificationOutboxEntity> list(int page) {
        return repository.findByOrderByIdDesc(PageRequest.of(Math.max(0, page), PAGE_SIZE));
    }

    public long countSentByCheckId(Long checkId) {
        return repository.countSentByCheckId(checkId, NotificationStatus.SENT);
    }
}
