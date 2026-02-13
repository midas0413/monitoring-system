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

        // 기존 링크 조회 (recipient를 함께 fetch하여 recipientId를 확실히 얻기)
        List<VpnRecipientLinkEntity> existingLinks = linkRepo.findByVpnIdWithRecipient(vpnId);
        Map<Long, VpnRecipientLinkEntity> existingLinkMap = new HashMap<>();
        for (VpnRecipientLinkEntity link : existingLinks) {
            // recipient를 함께 fetch했으므로 recipientId를 확실히 얻을 수 있음
            Long recipientId = link.getRecipient() != null ? link.getRecipient().getId() : link.getRecipientId();
            if (recipientId != null) {
                existingLinkMap.put(recipientId, link);
            }
        }

        // 새로 추가할 recipient ID 집합
        Set<Long> newRecipientIdSet = new HashSet<>();
        if (newRecipientIds != null) {
            for (Long rid : newRecipientIds) {
                if (rid != null) {
                    newRecipientIdSet.add(rid);
                }
            }
        }

        // 기존 링크 중 새 목록에 없는 것은 삭제
        for (VpnRecipientLinkEntity existingLink : existingLinks) {
            Long recipientId = existingLink.getRecipient() != null ? existingLink.getRecipient().getId() : existingLink.getRecipientId();
            if (recipientId != null && !newRecipientIdSet.contains(recipientId)) {
                linkRepo.delete(existingLink);
                log.debug("VPN recipient link 삭제: vpnId={}, recipientId={}", vpnId, recipientId);
            }
        }
        
        // 삭제 후 flush하여 DB에 반영
        linkRepo.flush();

        // 새 목록에 있는 recipient에 대해 링크 추가 또는 업데이트
        for (Long rid : newRecipientIdSet) {
            AlertRecipientEntity recipient = recipientRepo.findById(rid).orElse(null);
            if (recipient == null) {
                log.warn("Recipient가 존재하지 않습니다. recipientId={}, vpnId={}", rid, vpnId);
                continue;
            }

            // 저장 전에 다시 확인 (삭제 후 flush했으므로)
            VpnRecipientLinkEntity link = existingLinkMap.get(rid);
            if (link == null) {
                // 존재 여부를 다시 확인
                link = linkRepo.findByVpnIdAndRecipient_Id(vpnId, rid).orElse(null);
                if (link == null) {
                    // 새로 생성
                    link = new VpnRecipientLinkEntity();
                    link.setVpnId(vpnId);
                    link.setRecipient(recipient);
                    link.setEnabled(true);
                    linkRepo.save(link);
                    log.debug("VPN recipient link 생성: vpnId={}, recipientId={}", vpnId, rid);
                } else {
                    // 삭제되지 않고 남아있는 경우 (다른 트랜잭션에서 추가되었을 수 있음)
                    if (!Boolean.TRUE.equals(link.getEnabled())) {
                        link.setEnabled(true);
                        linkRepo.save(link);
                        log.debug("VPN recipient link 활성화: vpnId={}, recipientId={}", vpnId, rid);
                    }
                }
            } else {
                // 기존 링크가 있으면 활성화 상태만 업데이트
                if (!Boolean.TRUE.equals(link.getEnabled())) {
                    link.setEnabled(true);
                    linkRepo.save(link);
                    log.debug("VPN recipient link 활성화: vpnId={}, recipientId={}", vpnId, rid);
                }
            }
        }

        log.info("VPN recipients 저장 완료: vpnId={}, 총 {}개 링크", vpnId, newRecipientIdSet.size());
    }
}
