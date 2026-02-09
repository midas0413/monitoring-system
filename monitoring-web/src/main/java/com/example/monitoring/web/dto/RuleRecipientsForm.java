package com.example.monitoring.web.dto;

import java.util.ArrayList;
import java.util.List;

public class RuleRecipientsForm {

    // 체크된 recipientId 목록
    private List<Long> recipientIds = new ArrayList<>();

    public List<Long> getRecipientIds() { return recipientIds; }
    public void setRecipientIds(List<Long> recipientIds) { this.recipientIds = recipientIds; }
}