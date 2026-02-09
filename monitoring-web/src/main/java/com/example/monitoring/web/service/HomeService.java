package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.NotificationStatus;
import com.example.monitoring.common.repo.CheckRepository;
import com.example.monitoring.common.repo.NotificationOutboxRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 홈 대시보드용 데이터 조회 (Check 기준)
 */
@Service
public class HomeService {

    private final CheckRepository checkRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;

    public HomeService(CheckRepository checkRepository,
                       NotificationOutboxRepository notificationOutboxRepository) {
        this.checkRepository = checkRepository;
        this.notificationOutboxRepository = notificationOutboxRepository;
    }

    public List<CheckEntity> listAllChecks() {
        return checkRepository.findAll();
    }

    /** targetName별로 checks 그룹화 */
    public Map<String, List<CheckEntity>> listChecksGroupByTarget() {
        return checkRepository.findAll().stream()
                .collect(Collectors.groupingBy(c -> c.getTargetName() != null ? c.getTargetName() : "(미지정)"));
    }

    public long countSentNotificationsByCheck(Long checkId) {
        return notificationOutboxRepository.countSentByCheckId(checkId, NotificationStatus.SENT);
    }
}
