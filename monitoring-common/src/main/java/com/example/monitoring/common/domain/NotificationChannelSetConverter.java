package com.example.monitoring.common.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class NotificationChannelSetConverter implements AttributeConverter<Set<NotificationChannel>, String> {

    @Override
    public String convertToDatabaseColumn(Set<NotificationChannel> attribute) {
        if (attribute == null || attribute.isEmpty()) return "";
        return attribute.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<NotificationChannel> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptySet();
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(NotificationChannel::valueOf)
                .collect(Collectors.toSet());
    }
}