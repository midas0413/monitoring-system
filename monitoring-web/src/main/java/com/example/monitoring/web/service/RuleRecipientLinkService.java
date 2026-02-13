package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import com.example.monitoring.common.domain.AlertRuleRecipientLinkEntity;
import com.example.monitoring.common.repo.AlertRecipientRepository;
import com.example.monitoring.common.repo.AlertRuleRecipientLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RuleRecipientLinkService {

    private static final Logger log = LoggerFactory.getLogger(RuleRecipientLinkService.class);

    private final AlertRecipientRepository recipientRepo;
    private final AlertRuleRecipientLinkRepository linkRepo;

    public RuleRecipientLinkService(AlertRecipientRepository recipientRepo,
                                    AlertRuleRecipientLinkRepository linkRepo) {
        this.recipientRepo = recipientRepo;
        this.linkRepo = linkRepo;
    }

    @Transactional(readOnly = true)
    public List<AlertRecipientEntity> listRecipients() {
        return recipientRepo.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Set<Long> linkedRecipientIds(Long ruleId) {
        List<AlertRuleRecipientLinkEntity> links = linkRepo.findByRuleId(ruleId);
        Set<Long> ids = new HashSet<>();
        for (AlertRuleRecipientLinkEntity l : links) {
            if (Boolean.TRUE.equals(l.getEnabled())) ids.add(l.getRecipientId());
        }
        return ids;
    }

    /**
     * 규칙에 연결된 활성화된 수신자 수 조회
     */
    @Transactional(readOnly = true)
    public int countLinkedRecipients(Long ruleId) {
        List<AlertRuleRecipientLinkEntity> links = linkRepo.findByRuleId(ruleId);
        int count = 0;
        for (AlertRuleRecipientLinkEntity l : links) {
            if (Boolean.TRUE.equals(l.getEnabled())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 여러 규칙에 대한 수신자 수 맵 생성
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> buildRecipientCountMap(List<Long> ruleIds) {
        Map<Long, Integer> map = new HashMap<>();
        for (Long ruleId : ruleIds) {
            map.put(ruleId, countLinkedRecipients(ruleId));
        }
        return map;
    }

    @Transactional
    public void saveLinks(Long ruleId, List<Long> newRecipientIds) {
        log.info("Rule recipients 저장 시작: ruleId={}, recipientIds={}", ruleId, newRecipientIds);

        // 기존 링크 조회 (recipient를 함께 fetch하여 recipientId를 확실히 얻기)
        List<AlertRuleRecipientLinkEntity> existingLinks = linkRepo.findByRuleIdWithRecipient(ruleId);
        Map<Long, AlertRuleRecipientLinkEntity> existingLinkMap = new HashMap<>();
        for (AlertRuleRecipientLinkEntity link : existingLinks) {
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
        for (AlertRuleRecipientLinkEntity existingLink : existingLinks) {
            Long recipientId = existingLink.getRecipient() != null ? existingLink.getRecipient().getId() : existingLink.getRecipientId();
            if (recipientId != null && !newRecipientIdSet.contains(recipientId)) {
                linkRepo.delete(existingLink);
                log.debug("Rule recipient link 삭제: ruleId={}, recipientId={}", ruleId, recipientId);
            }
        }
        
        // 삭제 후 flush하여 DB에 반영
        linkRepo.flush();

        int savedCount = 0;
        int skippedCount = 0;

        // 새 목록에 있는 recipient에 대해 링크 추가 또는 업데이트
        for (Long rid : newRecipientIdSet) {
            AlertRecipientEntity recipient = recipientRepo.findById(rid).orElse(null);
            if (recipient == null) {
                log.warn("Recipient가 존재하지 않습니다. recipientId={}, ruleId={}", rid, ruleId);
                skippedCount++;
                continue;
            }

            // 저장 전에 다시 확인 (삭제 후 flush했으므로)
            AlertRuleRecipientLinkEntity link = existingLinkMap.get(rid);
            if (link == null) {
                // 존재 여부를 다시 확인
                link = linkRepo.findByRuleIdAndRecipient_Id(ruleId, rid).orElse(null);
                if (link == null) {
                    // 새로 생성
                    link = new AlertRuleRecipientLinkEntity();
                    link.setRuleId(ruleId);
                    link.setRecipient(recipient);
                    link.setEnabled(true);
                    linkRepo.save(link);
                    savedCount++;
                    log.debug("Rule recipient link 생성: ruleId={}, recipientId={}", ruleId, rid);
                } else {
                    // 삭제되지 않고 남아있는 경우 (다른 트랜잭션에서 추가되었을 수 있음)
                    if (!Boolean.TRUE.equals(link.getEnabled())) {
                        link.setEnabled(true);
                        linkRepo.save(link);
                        log.debug("Rule recipient link 활성화: ruleId={}, recipientId={}", ruleId, rid);
                    }
                }
            } else {
                // 기존 링크가 있으면 활성화 상태만 업데이트
                if (!Boolean.TRUE.equals(link.getEnabled())) {
                    link.setEnabled(true);
                    linkRepo.save(link);
                    log.debug("Rule recipient link 활성화: ruleId={}, recipientId={}", ruleId, rid);
                }
            }
        }

        log.info("Rule recipients 저장 완료: ruleId={}, saved={}, skipped={}, 총 {}개 링크", 
                ruleId, savedCount, skippedCount, newRecipientIdSet.size());
    }
}