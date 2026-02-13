package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.VpnConnectionEntity;
import com.example.monitoring.common.repo.VpnConnectionRepository;
import com.example.monitoring.web.dto.VpnConnectionForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class VpnConnectionService {

    private final VpnConnectionRepository vpnRepo;

    public VpnConnectionService(VpnConnectionRepository vpnRepo) {
        this.vpnRepo = vpnRepo;
    }

    @Transactional(readOnly = true)
    public List<VpnConnectionEntity> listEnabled() {
        return vpnRepo.findByEnabledTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<VpnConnectionEntity> list(String q) {
        List<VpnConnectionEntity> vpns;
        if (StringUtils.hasText(q)) {
            vpns = vpnRepo.findAll().stream()
                    .filter(vpn -> vpn.getName().toLowerCase().contains(q.toLowerCase()) ||
                                 (vpn.getHost() != null && vpn.getHost().toLowerCase().contains(q.toLowerCase())))
                    .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                    .toList();
        } else {
            vpns = vpnRepo.findAll().stream()
                    .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                    .toList();
        }
        // DB에서 조회한 시간을 한국 시간(KST, UTC+9)으로 변환
        convertToKst(vpns);
        return vpns;
    }

    @Transactional(readOnly = true)
    public VpnConnectionEntity get(Long id) {
        VpnConnectionEntity vpn = vpnRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("VPN connection not found: " + id));
        // DB에서 조회한 시간을 한국 시간(KST, UTC+9)으로 변환
        convertToKst(List.of(vpn));
        return vpn;
    }

    @Transactional
    public Long create(VpnConnectionForm form) {
        if (vpnRepo.findByName(form.getName()).isPresent()) {
            throw new IllegalArgumentException("VPN connection with name '" + form.getName() + "' already exists");
        }

        VpnConnectionEntity entity = new VpnConnectionEntity();
        applyForm(entity, form);
        vpnRepo.save(entity);
        return entity.getId();
    }

    @Transactional
    public void update(Long id, VpnConnectionForm form) {
        VpnConnectionEntity entity = get(id);
        
        // 이름 변경 시 중복 체크
        if (!entity.getName().equals(form.getName()) && 
            vpnRepo.findByName(form.getName()).isPresent()) {
            throw new IllegalArgumentException("VPN connection with name '" + form.getName() + "' already exists");
        }

        applyForm(entity, form);
        vpnRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        VpnConnectionEntity entity = get(id);
        vpnRepo.delete(entity);
    }

    @Transactional(readOnly = true)
    public VpnConnectionForm toForm(VpnConnectionEntity entity) {
        VpnConnectionForm form = new VpnConnectionForm();
        form.setId(entity.getId());
        form.setName(entity.getName());
        form.setHost(entity.getHost());
        form.setCheckIntervalSec(entity.getCheckIntervalSec());
        form.setCheckMethod(entity.getCheckMethod() != null ? entity.getCheckMethod() : com.example.monitoring.common.domain.VpnCheckMethod.TCP);
        form.setKakaoTemplateCode(entity.getKakaoTemplateCode());
        form.setKakaoTemplateVariables(entity.getKakaoTemplateVariables());
        form.setEnabled(entity.getEnabled());
        form.setDescription(entity.getDescription());
        return form;
    }

    private void applyForm(VpnConnectionEntity entity, VpnConnectionForm form) {
        entity.setName(StringUtils.hasText(form.getName()) ? form.getName().trim() : null);
        entity.setHost(StringUtils.hasText(form.getHost()) ? form.getHost().trim() : null);
        entity.setCheckIntervalSec(form.getCheckIntervalSec() != null ? form.getCheckIntervalSec() : 60);
        entity.setCheckMethod(form.getCheckMethod() != null ? form.getCheckMethod() : com.example.monitoring.common.domain.VpnCheckMethod.TCP);
        entity.setKakaoTemplateCode(form.getKakaoTemplateCode());
        entity.setKakaoTemplateVariables(form.getKakaoTemplateVariables());
        entity.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);
        entity.setDescription(StringUtils.hasText(form.getDescription()) ? form.getDescription().trim() : null);
    }

    /**
     * VPN 엔티티의 시간 필드를 한국 시간(KST, UTC+9)으로 변환
     */
    private void convertToKst(List<VpnConnectionEntity> vpns) {
        ZoneId kstZone = ZoneId.of("Asia/Seoul");
        for (VpnConnectionEntity vpn : vpns) {
            if (vpn.getLastCheckedAt() != null) {
                vpn.setLastCheckedAt(vpn.getLastCheckedAt().atZoneSameInstant(kstZone).toOffsetDateTime());
            }
            if (vpn.getLastStatusChangeAt() != null) {
                vpn.setLastStatusChangeAt(vpn.getLastStatusChangeAt().atZoneSameInstant(kstZone).toOffsetDateTime());
            }
        }
    }
}
