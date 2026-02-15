package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "servers")
public class ServerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "server_purpose", nullable = false, length = 20)
    private ServerPurpose serverPurpose;

    @Column(nullable = false)
    private Boolean enabled = true;

    /** 연결상태: 주기 ping 체크 결과로 갱신 */
    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false, length = 20)
    private ServerStatus connectionStatus = ServerStatus.UNKNOWN;

    /** 연결상태 체크 주기(초). 0이면 주기 체크 미사용 */
    @Column(name = "connection_check_interval_sec", nullable = false)
    private Integer connectionCheckIntervalSec = 0;

    /** 마지막 연결상태(ping) 체크 시각 */
    @Column(name = "last_connection_check_at")
    private OffsetDateTime lastConnectionCheckAt;

    @Column(length = 500)
    private String description;

    // SSH 연결 정보 (SHELL, LOGS, DISK_SPACE 모니터링용)
    @Column(name = "ssh_port")
    private Integer sshPort = 22;

    @Column(name = "ssh_username", length = 100)
    private String sshUsername;

    @Column(name = "ssh_password", columnDefinition = "text")
    private String sshPassword;

    @Column(name = "ssh_private_key_path", columnDefinition = "text")
    private String sshPrivateKeyPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public ServerPurpose getServerPurpose() { return serverPurpose; }
    public void setServerPurpose(ServerPurpose serverPurpose) { this.serverPurpose = serverPurpose; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public ServerStatus getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(ServerStatus connectionStatus) { this.connectionStatus = connectionStatus; }

    public Integer getConnectionCheckIntervalSec() { return connectionCheckIntervalSec; }
    public void setConnectionCheckIntervalSec(Integer connectionCheckIntervalSec) { this.connectionCheckIntervalSec = connectionCheckIntervalSec; }

    public OffsetDateTime getLastConnectionCheckAt() { return lastConnectionCheckAt; }
    public void setLastConnectionCheckAt(OffsetDateTime lastConnectionCheckAt) { this.lastConnectionCheckAt = lastConnectionCheckAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSshPort() { return sshPort; }
    public void setSshPort(Integer sshPort) { this.sshPort = sshPort; }

    public String getSshUsername() { return sshUsername; }
    public void setSshUsername(String sshUsername) { this.sshUsername = sshUsername; }

    public String getSshPassword() { return sshPassword; }
    public void setSshPassword(String sshPassword) { this.sshPassword = sshPassword; }

    public String getSshPrivateKeyPath() { return sshPrivateKeyPath; }
    public void setSshPrivateKeyPath(String sshPrivateKeyPath) { this.sshPrivateKeyPath = sshPrivateKeyPath; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
