package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import com.example.monitoring.common.domain.AlertRuleRecipientLinkEntity;
import com.example.monitoring.common.repo.AlertRecipientRepository;
import com.example.monitoring.common.repo.AlertRuleRecipientLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RuleRecipientLinkService {

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

    @Transactional
    public void saveLinks(Long ruleId, List<Long> newRecipientIds) {
        // 전략: ruleId 기준으로 "전체 갱신" (가장 단순/안정)
        linkRepo.deleteByRuleId(ruleId);

        if (newRecipientIds == null) return;
        for (Long rid : newRecipientIds) {
            if (rid == null) continue;
            AlertRuleRecipientLinkEntity link = new AlertRuleRecipientLinkEntity();
            link.setRuleId(ruleId);
            link.setRecipient(recipientRepo.getReferenceById(rid));  // alert_recipients 참조
            link.setEnabled(true);
            linkRepo.save(link);
        }
    }
}