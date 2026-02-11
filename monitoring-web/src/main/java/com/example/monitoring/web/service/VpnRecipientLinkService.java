package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import com.example.monitoring.common.domain.VpnRecipientLinkEntity;
import com.example.monitoring.common.repo.AlertRecipientRepository;
import com.example.monitoring.common.repo.VpnRecipientLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class VpnRecipientLinkService {

    private static final Logger log = LoggerFactory.getLogger(VpnRecipientLinkService.class);

    private final AlertRecipientRepository recipientRepo;
    private final VpnRecipientLinkRepository linkRepo;

    public VpnRecipientLinkService(AlertRecipientRepository recipientRepo,
                                    VpnRecipientLinkRepository linkRepo) {
        this.recipientRepo = recipientRepo;
        this.linkRepo = linkRepo;
    }

    @Transactional(readOnly = true)
    public List<AlertRecipientEntity> listRecipients() {
        return recipientRepo.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Set<Long> linkedRecipientIds(Long vpnId) {
        List<VpnRecipientLinkEntity> links = linkRepo.findByVpnId(vpnId);
        Set<Long> ids = new HashSet<>();
        for (VpnRecipientLinkEntity l : links) {
            if (Boolean.TRUE.equals(l.getEnabled())) {
                ids.add(l.getRecipientId());
            }
        }
        return ids;
    }

    @Transactional(readOnly = true)
    public int countLinkedRecipients(Long vpnId) {
        List<VpnRecipientLinkEntity> links = linkRepo.findByVpnId(vpnId);
        int count = 0;
        for (VpnRecipientLinkEntity l : links) {
            if (Boolean.TRUE.equals(l.getEnabled())) {
                count++;
            }
        }
        return count;
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> buildRecipientCountMap(List<Long> vpnIds) {
        Map<Long, Integer> map = new HashMap<>();
        for (Long vpnId : vpnIds) {
            map.put(vpnId, countLinkedRecipients(vpnId));
        }
        return map;
    }

    @Transactional
    public void saveLinks(Long vpnId, List<Long> newRecipientIds) {
        log.info("VPN recipients 저장 시작: vpnId={}, recipientIds={}", vpnId, newRecipientIds);

        try {
            linkRepo.deleteByVpnId(vpnId);
            log.debug("기존 VPN 수신자 링크 삭제 완료: vpnId={}", vpnId);
        } catch (Exception e) {
            log.warn("기존 링크 삭제 중 오류 (무시 가능): vpnId={}", vpnId, e);
        }

        if (newRecipientIds == null || newRecipientIds.isEmpty()) {
            log.info("VPN recipients 저장 완료: vpnId={}, recipientIds=null 또는 empty", vpnId);
            return;
        }

        for (Long rid : newRecipientIds) {
            if (rid == null) continue;
            AlertRecipientEntity recipient = recipientRepo.findById(rid).orElse(null);
            if (recipient == null) {
                log.warn("Recipient가 존재하지 않습니다. recipientId={}, vpnId={}", rid, vpnId);
                continue;
            }
            try {
                VpnRecipientLinkEntity link = new VpnRecipientLinkEntity();
                link.setVpnId(vpnId);
                link.setRecipient(recipient);
                link.setEnabled(true);
                linkRepo.save(link);
            } catch (Exception e) {
                log.error("VPN recipient link 저장 실패: vpnId={}, recipientId={}", vpnId, rid, e);
                throw e;
            }
        }
        log.info("VPN recipients 저장 완료: vpnId={}", vpnId);
    }
}
