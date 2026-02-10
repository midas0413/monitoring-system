package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.VpnConnectionEntity;
import com.example.monitoring.common.repo.VpnConnectionRepository;
import com.example.monitoring.web.dto.VpnConnectionForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        if (StringUtils.hasText(q)) {
            return vpnRepo.findAll().stream()
                    .filter(vpn -> vpn.getName().toLowerCase().contains(q.toLowerCase()) ||
                                 (vpn.getHost() != null && vpn.getHost().toLowerCase().contains(q.toLowerCase())))
                    .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                    .toList();
        }
        return vpnRepo.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public VpnConnectionEntity get(Long id) {
        return vpnRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("VPN connection not found: " + id));
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
        form.setEnabled(entity.getEnabled());
        form.setDescription(entity.getDescription());
        return form;
    }

    private void applyForm(VpnConnectionEntity entity, VpnConnectionForm form) {
        entity.setName(StringUtils.hasText(form.getName()) ? form.getName().trim() : null);
        entity.setHost(StringUtils.hasText(form.getHost()) ? form.getHost().trim() : null);
        entity.setCheckIntervalSec(form.getCheckIntervalSec() != null ? form.getCheckIntervalSec() : 60);
        entity.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);
        entity.setDescription(StringUtils.hasText(form.getDescription()) ? form.getDescription().trim() : null);
    }
}
