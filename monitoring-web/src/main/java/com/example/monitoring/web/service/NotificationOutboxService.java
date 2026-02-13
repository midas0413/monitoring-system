package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.common.domain.NotificationOutboxEntity;
import com.example.monitoring.common.domain.NotificationStatus;
import com.example.monitoring.common.repo.CheckRunRepository;
import com.example.monitoring.common.repo.MonitoringRuleRepository;
import com.example.monitoring.common.repo.NotificationOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationOutboxService {

    private static final int PAGE_SIZE = 100;
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationOutboxRepository repository;
    private final MonitoringRuleRepository monitoringRuleRepository;
    private final CheckRunRepository checkRunRepository;

    public NotificationOutboxService(NotificationOutboxRepository repository,
                                    MonitoringRuleRepository monitoringRuleRepository,
                                    CheckRunRepository checkRunRepository) {
        this.repository = repository;
        this.monitoringRuleRepository = monitoringRuleRepository;
        this.checkRunRepository = checkRunRepository;
    }

    public List<NotificationOutboxEntity> list(int page) {
        return repository.findByOrderByIdDesc(PageRequest.of(Math.max(0, page), PAGE_SIZE));
    }

    public long countSentByCheckId(Long checkId) {
        return repository.countSentByCheckId(checkId, NotificationStatus.SENT);
    }

    /** outboxId -> Asia/Seoul 타임존 기준 Created 포맷 문자열 */
    public Map<Long, String> buildCreatedDisplayMap(List<NotificationOutboxEntity> items) {
        Map<Long, String> map = new HashMap<>();
        for (NotificationOutboxEntity n : items) {
            if (n.getCreatedAt() == null) {
                map.put(n.getId(), "-");
                continue;
            }
            // OffsetDateTime을 Asia/Seoul 타임존으로 변환
            // OffsetDateTime은 UTC 또는 시스템 타임존으로 저장될 수 있으므로
            // atZoneSameInstant를 사용하여 정확히 변환
            try {
                String formatted = n.getCreatedAt().atZoneSameInstant(SEOUL_ZONE).format(DATE_TIME_FORMATTER);
                map.put(n.getId(), formatted);
            } catch (Exception e) {
                // 변환 실패 시 원본 값 사용
                map.put(n.getId(), n.getCreatedAt().toString());
            }
        }
        return map;
    }

    /** 
     * monitoringRuleId -> "(ID) 규칙명" 형식의 문자열
     */
    public Map<Long, String> buildRuleDisplayMap(List<NotificationOutboxEntity> items) {
        Map<Long, String> map = new HashMap<>();
        for (NotificationOutboxEntity n : items) {
            Long ruleId = n.getMonitoringRuleId();
            if (ruleId == null) {
                continue; // null인 경우 건너뛰기
            }
            if (map.containsKey(ruleId)) continue;
            
            String ruleDisplay = monitoringRuleRepository.findById(ruleId)
                    .map(rule -> {
                        String ruleName = rule.getName() != null ? rule.getName() : "-";
                        return "(" + ruleId + ") " + ruleName;
                    })
                    .orElse("(" + ruleId + ") -");
            map.put(ruleId, ruleDisplay);
        }
        return map;
    }

    /**
     * checkRunId -> 규칙명 형식의 문자열 (VPN 상태변경 등)
     * VPN 상태변경의 경우 checkRunId를 통해 vpnId를 확인하여 "VPN 상태변경"으로 표시
     */
    public Map<Long, String> buildCheckRunRuleDisplayMap(List<NotificationOutboxEntity> items) {
        Map<Long, String> map = new HashMap<>();
        
        // checkRunId가 있고 monitoringRuleId가 null인 항목들의 checkRunId 수집
        Set<Long> checkRunIds = items.stream()
                .filter(n -> n.getCheckRunId() != null && n.getMonitoringRuleId() == null)
                .map(NotificationOutboxEntity::getCheckRunId)
                .collect(Collectors.toSet());
        
        // checkRunId로 CheckRunEntity 조회하여 VPN 상태변경인지 확인
        for (Long checkRunId : checkRunIds) {
            if (map.containsKey(checkRunId)) continue;
            
            checkRunRepository.findById(checkRunId).ifPresent(checkRun -> {
                if (checkRun.getVpnId() != null) {
                    // VPN 상태변경인 경우
                    map.put(checkRunId, "VPN 상태변경");
                }
            });
        }
        
        return map;
    }
}
