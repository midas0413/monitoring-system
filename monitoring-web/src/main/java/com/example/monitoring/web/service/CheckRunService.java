package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.repo.CheckRunRepository;
import com.example.monitoring.common.repo.CheckRepository;
import com.example.monitoring.web.dto.CheckRunCreateRequest;
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

    private static final int PAGE_SIZE = 100;

    private final CheckRunRepository checkRunRepository;
    private final CheckRepository checkRepository;

    public CheckRunService(CheckRunRepository checkRunRepository, CheckRepository checkRepository) {
        this.checkRunRepository = checkRunRepository;
        this.checkRepository = checkRepository;
    }

    public CheckRunEntity create(CheckRunCreateRequest req) {
        if (!checkRepository.existsById(req.getCheckId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkId not found: " + req.getCheckId());
        }

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
            if (r.getCheckId() != null) {
                tz = checkRepository.findById(r.getCheckId())
                        .map(c -> c.getTimezone())
                        .filter(t -> t != null && !t.isBlank())
                        .orElse(null);
            }
            ZoneId zone = (tz != null) ? ZoneId.of(tz) : ZoneId.systemDefault();
            String formatted = r.getStartedAt().atZoneSameInstant(zone).format(fmt);
            map.put(r.getId(), formatted);
        }
        return map;
    }
}
