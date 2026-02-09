package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="alert_recipients")
public class AlertRecipientEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=100)
    private String name;

    @Column(nullable=false)
    private Boolean enabled = true;

    // CSV: "SMS,EMAIL"
    @Column(nullable=false, length=100)
    private String channels;

    @Column(length=50)
    private String phone;

    @Column(length=200)
    private String email;

    @Column(length=200)
    private String kakao;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getChannels() { return channels; }
    public void setChannels(String channels) { this.channels = channels; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getKakao() { return kakao; }
    public void setKakao(String kakao) { this.kakao = kakao; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}