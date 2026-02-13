package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.AlertRuleRecipientLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertRuleRecipientLinkRepository extends JpaRepository<AlertRuleRecipientLinkEntity, Long> {

    @Query("SELECT l FROM AlertRuleRecipientLinkEntity l JOIN FETCH l.recipient WHERE l.ruleId = :ruleId AND l.enabled = true")
    List<AlertRuleRecipientLinkEntity> findByRuleIdAndEnabledTrue(@Param("ruleId") Long ruleId);

    @Query("SELECT l FROM AlertRuleRecipientLinkEntity l JOIN FETCH l.recipient WHERE l.ruleId = :ruleId")
    List<AlertRuleRecipientLinkEntity> findByRuleIdWithRecipient(@Param("ruleId") Long ruleId);

    List<AlertRuleRecipientLinkEntity> findByRuleId(Long ruleId);
    Optional<AlertRuleRecipientLinkEntity> findByRuleIdAndRecipient_Id(Long ruleId, Long recipientId);

    void deleteByRuleId(Long ruleId);
}