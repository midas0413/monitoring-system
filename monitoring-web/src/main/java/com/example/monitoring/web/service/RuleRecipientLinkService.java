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
        
        // 전략: ruleId 기준으로 "전체 갱신" (가장 단순/안정)
        try {
            linkRepo.deleteByRuleId(ruleId);
            log.debug("기존 링크 삭제 완료: ruleId={}", ruleId);
        } catch (Exception e) {
            log.warn("기존 링크 삭제 중 오류 (무시 가능): ruleId={}", ruleId, e);
        }

        if (newRecipientIds == null || newRecipientIds.isEmpty()) {
            log.info("Rule recipients 저장 완료: ruleId={}, recipientIds=null 또는 empty", ruleId);
            return;
        }
        
        int savedCount = 0;
        int skippedCount = 0;
        
        for (Long rid : newRecipientIds) {
            if (rid == null) {
                skippedCount++;
                log.debug("null recipientId 스킵: ruleId={}", ruleId);
                continue;
            }
            
            // getReferenceById 대신 findById 사용하여 존재 여부 확인
            // getReferenceById는 존재하지 않는 경우에도 lazy proxy를 반환하여 나중에 오류 발생 가능
            AlertRecipientEntity recipient = recipientRepo.findById(rid).orElse(null);
            if (recipient == null) {
                log.warn("Recipient가 존재하지 않습니다. recipientId={}, ruleId={}", rid, ruleId);
                skippedCount++;
                continue;
            }
            
            try {
                AlertRuleRecipientLinkEntity link = new AlertRuleRecipientLinkEntity();
                link.setRuleId(ruleId);
                link.setRecipient(recipient);
                link.setEnabled(true);
                linkRepo.save(link);
                savedCount++;
                log.debug("Rule recipient link 저장 완료: ruleId={}, recipientId={}, linkId={}", 
                        ruleId, rid, link.getId());
            } catch (Exception e) {
                log.error("Rule recipient link 저장 실패: ruleId={}, recipientId={}", ruleId, rid, e);
                throw e; // 트랜잭션 롤백을 위해 예외 재발생
            }
        }
        
        log.info("Rule recipients 저장 완료: ruleId={}, saved={}, skipped={}", ruleId, savedCount, skippedCount);
    }
}