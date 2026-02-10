package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.CheckRunEntity;
// import com.example.monitoring.common.repo.AlertRuleRepository;  // Deprecated
import com.example.monitoring.common.repo.CheckRunRepository;
// import com.example.monitoring.common.repo.CheckRepository;  // Deprecated
import com.example.monitoring.web.dto.CheckRunCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckRunService {

    private static final Logger log = LoggerFactory.getLogger(CheckRunService.class);

    private static final int PAGE_SIZE = 100;

    private final CheckRunRepository checkRunRepository;
    // Deprecated: CheckRepository와 AlertRuleRepository는 더 이상 사용되지 않음
    // private final CheckRepository checkRepository;
    // private final AlertRuleRepository alertRuleRepository;

    public CheckRunService(CheckRunRepository checkRunRepository/*, CheckRepository checkRepository, AlertRuleRepository alertRuleRepository*/) {
        this.checkRunRepository = checkRunRepository;
        // this.checkRepository = checkRepository;
        // this.alertRuleRepository = alertRuleRepository;
    }

    public CheckRunEntity create(CheckRunCreateRequest req) {
        // Deprecated: CheckRepository 사용 불가
        // if (!checkRepository.existsById(req.getCheckId())) {
        //     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkId not found: " + req.getCheckId());
        // }

        CheckRunEntity run = new CheckRunEntity();
        run.setCheckId(req.getCheckId());
        run.setSuccess(req.getSuccess());
        run.setDurationMs(req.getDurationMs());
        run.setOutput(req.getOutput());
        run.setErrorMessage(req.getErrorMessage());
        run.setFinishedAt(OffsetDateTime.now());

        return checkRunRepository.save(run);
    }

    public CheckRunEntity get(Long id) {
        return checkRunRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CheckRun not found: " + id));
    }

    public List<CheckRunEntity> list(Long checkId, Long ignoredServerId) {
        if (checkId != null) {
            return checkRunRepository.findTop100ByCheckIdOrderByStartedAtDesc(checkId);
        }
        return checkRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, PAGE_SIZE));
    }

    /** runId -> 서버 타임존 기준 Started 포맷 문자열 */
    public Map<Long, String> buildStartedDisplayMap(List<CheckRunEntity> runs) {
        Map<Long, String> map = new HashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (CheckRunEntity r : runs) {
            if (r.getStartedAt() == null) {
                map.put(r.getId(), "-");
                continue;
            }
            String tz = null;
            // Deprecated: CheckRepository 사용 불가
            // if (r.getCheckId() != null) {
            //     tz = checkRepository.findById(r.getCheckId())
            //             .map(c -> c.getTimezone())
            //             .filter(t -> t != null && !t.isBlank())
            //             .orElse(null);
            // }
            
            // 타임존 변환 (잘못된 타임존 ID 처리)
            ZoneId zone;
            try {
                if (tz != null) {
                    // 잘못된 타임존 ID 수정 (예: Europe/Scopje -> Europe/Skopje)
                    if ("Europe/Scopje".equals(tz)) {
                        tz = "Europe/Skopje";
                    }
                    zone = ZoneId.of(tz);
                } else {
                    zone = ZoneId.systemDefault();
                }
            } catch (Exception e) {
                // 잘못된 타임존 ID인 경우 시스템 기본 타임존 사용
                log.warn("잘못된 타임존 ID: {}. 시스템 기본 타임존 사용. checkId={}", tz, r.getCheckId());
                zone = ZoneId.systemDefault();
            }
            
            String formatted = r.getStartedAt().atZoneSameInstant(zone).format(fmt);
            map.put(r.getId(), formatted);
        }
        return map;
    }

    /** checkId -> Rule Name (AlertRuleEntity의 name) */
    public Map<Long, String> buildRuleNameDisplayMap(List<CheckRunEntity> runs) {
        Map<Long, String> map = new HashMap<>();
        // Deprecated: AlertRuleRepository 사용 불가
        for (CheckRunEntity r : runs) {
            if (r.getCheckId() == null) continue;
            if (map.containsKey(r.getCheckId())) continue;
            // String ruleName = alertRuleRepository.findFirstByCheckId(r.getCheckId())
            //         .map(rule -> rule.getName() != null ? rule.getName() : "-")
            //         .orElse("-");
            map.put(r.getCheckId(), "-");  // 임시로 "-" 반환
        }
        return map;
    }

    /** checkId -> Server Name (CheckEntity의 targetName) */
    public Map<Long, String> buildServerDisplayMap(List<CheckRunEntity> runs) {
        Map<Long, String> map = new HashMap<>();
        // Deprecated: CheckRepository 사용 불가
        for (CheckRunEntity r : runs) {
            if (r.getCheckId() == null) continue;
            if (map.containsKey(r.getCheckId())) continue;
            // String serverName = checkRepository.findById(r.getCheckId())
            //         .map(c -> c.getTargetName() != null ? c.getTargetName() : "-")
            //         .orElse("-");
            map.put(r.getCheckId(), "-");  // 임시로 "-" 반환
        }
        return map;
    }
}
