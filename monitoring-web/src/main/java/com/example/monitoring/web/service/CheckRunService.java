package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.common.repo.CheckRunRepository;
import com.example.monitoring.common.repo.MonitoringRuleRepository;
import com.example.monitoring.common.repo.ServerRepository;
import com.example.monitoring.common.repo.VpnConnectionRepository;
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
    private final MonitoringRuleRepository monitoringRuleRepository;
    private final ServerRepository serverRepository;
    private final VpnConnectionRepository vpnConnectionRepository;

    public CheckRunService(CheckRunRepository checkRunRepository,
                          MonitoringRuleRepository monitoringRuleRepository,
                          ServerRepository serverRepository,
                          VpnConnectionRepository vpnConnectionRepository) {
        this.checkRunRepository = checkRunRepository;
        this.monitoringRuleRepository = monitoringRuleRepository;
        this.serverRepository = serverRepository;
        this.vpnConnectionRepository = vpnConnectionRepository;
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

    public List<CheckRunEntity> list(Long ruleId, Long ignoredServerId) {
        if (ruleId != null) {
            return checkRunRepository.findTop100ByMonitoringRuleIdOrderByStartedAtDesc(ruleId);
        }
        return checkRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, PAGE_SIZE));
    }

    /** runId -> 한국 시간(KST) 기준 Started 포맷 문자열 */
    public Map<Long, String> buildStartedDisplayMap(List<CheckRunEntity> runs) {
        Map<Long, String> map = new HashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        
        for (CheckRunEntity r : runs) {
            if (r.getStartedAt() == null) {
                map.put(r.getId(), "-");
                continue;
            }
            
            // 한국 시간으로 변환
            String formatted = r.getStartedAt().atZoneSameInstant(koreaZone).format(fmt);
            map.put(r.getId(), formatted);
        }
        return map;
    }

    /** runId -> 한국 시간(KST) 기준 Finished 포맷 문자열 */
    public Map<Long, String> buildFinishedDisplayMap(List<CheckRunEntity> runs) {
        Map<Long, String> map = new HashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        
        for (CheckRunEntity r : runs) {
            if (r.getFinishedAt() == null) {
                map.put(r.getId(), "-");
                continue;
            }
            
            // 한국 시간으로 변환
            String formatted = r.getFinishedAt().atZoneSameInstant(koreaZone).format(fmt);
            map.put(r.getId(), formatted);
        }
        return map;
    }

    /** monitoringRuleId -> Rule Name */
    public Map<Long, String> buildRuleNameDisplayMap(List<CheckRunEntity> runs) {
        Map<Long, String> map = new HashMap<>();
        for (CheckRunEntity r : runs) {
            Long ruleId = r.getMonitoringRuleId();
            if (ruleId == null) continue;
            if (map.containsKey(ruleId)) continue;
            
            String ruleName = monitoringRuleRepository.findById(ruleId)
                    .map(rule -> rule.getName() != null ? rule.getName() : "-")
                    .orElse("-");
            map.put(ruleId, ruleName);
        }
        return map;
    }

    /** monitoringRuleId -> Server Name */
    public Map<Long, String> buildServerDisplayMap(List<CheckRunEntity> runs) {
        Map<Long, String> map = new HashMap<>();
        for (CheckRunEntity r : runs) {
            Long ruleId = r.getMonitoringRuleId();
            if (ruleId == null) continue;
            if (map.containsKey(ruleId)) continue;
            
            String serverName = monitoringRuleRepository.findById(ruleId)
                    .map(rule -> {
                        if (rule.getServerId() == null) return "-";
                        return serverRepository.findById(rule.getServerId())
                                .map(server -> server.getName() != null ? server.getName() : "-")
                                .orElse("-");
                    })
                    .orElse("-");
            map.put(ruleId, serverName);
        }
        return map;
    }

    /** runId -> Rule Name (VPN 알림용: "VPN 상태변경") */
    public Map<Long, String> buildRuleNameByRunIdMap(List<CheckRunEntity> runs) {
        Map<Long, String> map = new HashMap<>();
        for (CheckRunEntity r : runs) {
            if (r.getVpnId() != null) {
                map.put(r.getId(), "VPN 상태변경");
            }
        }
        return map;
    }

    /** runId -> Server Name (VPN 알림용: VPN명) */
    public Map<Long, String> buildServerNameByRunIdMap(List<CheckRunEntity> runs) {
        Map<Long, String> map = new HashMap<>();
        for (CheckRunEntity r : runs) {
            if (r.getVpnId() != null) {
                String vpnName = vpnConnectionRepository.findById(r.getVpnId())
                        .map(vpn -> vpn.getName() != null ? vpn.getName() : "-")
                        .orElse("-");
                map.put(r.getId(), vpnName);
            }
        }
        return map;
    }
}
