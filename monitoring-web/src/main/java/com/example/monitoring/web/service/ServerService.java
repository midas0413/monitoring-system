package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.common.domain.ServerVpnLinkEntity;
import com.example.monitoring.common.domain.VpnConnectionEntity;
import com.example.monitoring.common.repo.ServerRepository;
import com.example.monitoring.common.repo.ServerVpnLinkRepository;
import com.example.monitoring.common.repo.VpnConnectionRepository;
import com.example.monitoring.web.dto.ServerForm;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ServerService {

    private static final Logger log = LoggerFactory.getLogger(ServerService.class);

    private final ServerRepository serverRepo;
    private final VpnConnectionRepository vpnRepo;
    private final ServerVpnLinkRepository linkRepo;
    private final EntityManager entityManager;

    public ServerService(ServerRepository serverRepo, 
                        VpnConnectionRepository vpnRepo,
                        ServerVpnLinkRepository linkRepo,
                        EntityManager entityManager) {
        this.serverRepo = serverRepo;
        this.vpnRepo = vpnRepo;
        this.linkRepo = linkRepo;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<ServerEntity> list(String q) {
        if (StringUtils.hasText(q)) {
            return serverRepo.findAll().stream()
                    .filter(s -> s.getName().toLowerCase().contains(q.toLowerCase()) 
                             || s.getHost().toLowerCase().contains(q.toLowerCase()))
                    .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                    .collect(Collectors.toList());
        }
        return serverRepo.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServerEntity get(Long id) {
        return serverRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Server not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ServerEntity> listEnabled() {
        return serverRepo.findByEnabledTrueOrderByNameAsc();
    }

    @Transactional
    public Long create(ServerForm form) {
        // 서버명 중복 체크
        if (serverRepo.existsByName(form.getName())) {
            throw new IllegalArgumentException("Server name already exists: " + form.getName());
        }

        ServerEntity entity = new ServerEntity();
        applyForm(entity, form);
        serverRepo.save(entity);

        // VPN 연결 저장
        saveVpnLinks(entity.getId(), form.getVpnIds());

        log.info("Server created: id={}, name={}", entity.getId(), entity.getName());
        return entity.getId();
    }

    @Transactional
    public void update(Long id, ServerForm form) {
        ServerEntity entity = get(id);
        
        // 서버명 변경 시 중복 체크
        if (!entity.getName().equals(form.getName()) && serverRepo.existsByName(form.getName())) {
            throw new IllegalArgumentException("Server name already exists: " + form.getName());
        }

        applyForm(entity, form);
        serverRepo.save(entity);

        // VPN 연결 업데이트: 기존 링크 삭제 후 새로 저장
        linkRepo.deleteByServerId(id);
        entityManager.flush(); // 삭제를 즉시 반영하여 중복 키 오류 방지
        saveVpnLinks(id, form.getVpnIds());

        log.info("Server updated: id={}, name={}", id, entity.getName());
    }

    @Transactional
    public void delete(Long id) {
        if (!serverRepo.existsById(id)) {
            throw new IllegalArgumentException("Server not found: " + id);
        }
        serverRepo.deleteById(id);
        log.info("Server deleted: id={}", id);
    }

    @Transactional(readOnly = true)
    public List<Long> getVpnIds(Long serverId) {
        return linkRepo.findByServerIdAndEnabledTrue(serverId).stream()
                .map(ServerVpnLinkEntity::getVpnId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Long, String> buildVpnDisplayMap(List<ServerEntity> servers) {
        Map<Long, String> map = new HashMap<>();
        for (ServerEntity server : servers) {
            List<ServerVpnLinkEntity> links = linkRepo.findByServerIdAndEnabledTrue(server.getId());
            if (links.isEmpty()) {
                map.put(server.getId(), "-");
            } else {
                List<String> vpnNames = links.stream()
                        .map(link -> vpnRepo.findById(link.getVpnId()).map(VpnConnectionEntity::getName).orElse("?"))
                        .collect(Collectors.toList());
                map.put(server.getId(), String.join(", ", vpnNames));
            }
        }
        return map;
    }

    private void applyForm(ServerEntity entity, ServerForm form) {
        entity.setName(form.getName());
        entity.setHost(form.getHost());
        entity.setTimezone(form.getTimezone());
        entity.setServerPurpose(form.getServerPurpose());
        entity.setEnabled(form.getEnabled());
        entity.setConnectionCheckIntervalSec(form.getConnectionCheckIntervalSec() != null ? form.getConnectionCheckIntervalSec() : 0);
        entity.setDescription(form.getDescription());
        
        // SSH 정보
        entity.setSshPort(form.getSshPort() != null ? form.getSshPort() : 22);
        entity.setSshUsername(form.getSshUsername());
        // 비밀번호는 빈 값이 아닐 때만 업데이트
        if (form.getSshPassword() != null && !form.getSshPassword().isBlank()) {
            entity.setSshPassword(form.getSshPassword());
        }
        entity.setSshPrivateKeyPath(form.getSshPrivateKeyPath());
    }

    private void saveVpnLinks(Long serverId, List<Long> vpnIds) {
        if (vpnIds == null || vpnIds.isEmpty()) {
            return;
        }

        for (Long vpnId : vpnIds) {
            if (vpnId == null) continue;
            
            // VPN 존재 여부 확인
            if (!vpnRepo.existsById(vpnId)) {
                log.warn("VPN not found: vpnId={}, skipping", vpnId);
                continue;
            }

            // 기존 링크 확인 (중복 방지)
            List<ServerVpnLinkEntity> existingLinks = linkRepo.findByServerId(serverId).stream()
                    .filter(link -> link.getVpnId().equals(vpnId))
                    .toList();
            
            if (!existingLinks.isEmpty()) {
                // 기존 링크가 있으면 활성화만 업데이트
                ServerVpnLinkEntity existing = existingLinks.get(0);
                existing.setEnabled(true);
                linkRepo.save(existing);
                log.debug("VPN link already exists, enabled: serverId={}, vpnId={}", serverId, vpnId);
            } else {
                // 기존 링크가 없으면 새로 생성
                ServerVpnLinkEntity link = new ServerVpnLinkEntity();
                link.setServerId(serverId);
                link.setVpnId(vpnId);
                link.setEnabled(true);
                linkRepo.save(link);
                log.debug("VPN link created: serverId={}, vpnId={}", serverId, vpnId);
            }
        }
    }
}
