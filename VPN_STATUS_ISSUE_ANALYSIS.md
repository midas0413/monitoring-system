# CARNM_VPN 상태변경 알림 및 UP/DOWN 반복 문제 분석

## 문제 상황

1. 시스템 기동 시 CARNM_VPN 상태변경 알림이 발송됨
2. 실행주기마다 UP/DOWN을 반복함
3. 실제로 10.10.0.91로 통신이 되지 않는데 UP 상태로 체크됨

## 원인 분석

### 1. 시스템 기동 시 알림 발송 원인

**코드 위치**: `VpnCheckService.java` line 84-106

```java
// 상태 변경 감지 (null인 경우도 변경으로 간주)
if (oldStatus != newStatus || oldStatus == null) {
    // ...
    // 상태 변경 알림 (oldStatus가 null이 아니고 실제로 변경된 경우에만)
    if (oldStatus != null && oldStatus != newStatus) {
        notifier.notifyStatusChange(freshVpn, oldStatus, newStatus);
    } else if (oldStatus == null) {
        log.info("Skipping notification for initial status: vpn={}, newStatus={}", 
                freshVpn.getName(), newStatus);
    }
}
```

**문제점**:
- VPN의 초기 상태가 `UNKNOWN`인 경우, `UNKNOWN` → `UP` 또는 `UNKNOWN` → `DOWN`으로 변경되면 알림이 발송됨
- 시스템 기동 시 첫 체크에서 `UNKNOWN` → `UP` 또는 `UNKNOWN` → `DOWN`으로 변경되면 알림 발송

**해결 방안**:
- 초기 상태(`UNKNOWN`)에서 변경되는 경우 알림을 발송하지 않도록 수정
- 또는 `UNKNOWN` 상태를 유지하고 실제 상태가 확정된 후에만 알림 발송

### 2. UP/DOWN 반복 원인

**코드 위치**: `VpnCheckService.java` line 208-315

**문제점**:
1. **포트가 지정되지 않은 경우**: 기본 포트(443, 80, 22, 8080)를 순서대로 체크
2. **하나라도 성공하면 UP**: 이 포트들은 VPN과 무관한 일반 서비스 포트
3. **10.10.0.91의 다른 서비스**: 웹 서버(80, 443), SSH(22) 등이 실행 중이면 UP으로 판정됨
4. **네트워크 불안정**: 간헐적으로 포트 연결이 성공/실패하면 UP/DOWN이 반복됨

**체크 로직**:
```java
// 기본 포트들 시도: 443, 80, 22, 8080 (HTTPS 우선)
portsToTest = new int[]{443, 80, 22, 8080};

for (int port : portsToTest) {
    socket.connect(new InetSocketAddress(hostname, port), CONNECTION_TIMEOUT_MS);
    // 성공하면 즉시 UP 반환
    return ServerStatus.UP;
}
```

### 3. 실제 통신이 안 되는데 UP으로 체크되는 원인

**가능한 시나리오**:
1. **10.10.0.91의 80번 포트가 열려있음**: 웹 서버가 실행 중
2. **10.10.0.91의 443번 포트가 열려있음**: HTTPS 서버가 실행 중
3. **10.10.0.91의 22번 포트가 열려있음**: SSH 서버가 실행 중
4. **VPN 서비스는 실행되지 않음**: 하지만 다른 서비스가 열려있어서 UP으로 판정

## 해결 방안

### 1. 초기 상태 알림 방지

```java
// UNKNOWN 상태에서 변경되는 경우 알림 발송하지 않음
if (oldStatus != null && oldStatus != ServerStatus.UNKNOWN && oldStatus != newStatus) {
    notifier.notifyStatusChange(freshVpn, oldStatus, newStatus);
}
```

### 2. VPN 전용 포트 지정

CARNM_VPN의 `host` 필드에 VPN 전용 포트를 명시:
- 예: `10.10.0.91:1194` (OpenVPN)
- 예: `10.10.0.91:443` (OpenVPN over TCP)

### 3. 체크 방법 변경

- `checkMethod`를 `PING`으로 변경하여 ICMP만 체크
- 또는 `BOTH`로 설정하여 TCP와 PING 모두 성공해야 UP으로 판정

### 4. 로그 확인

워커 애플리케이션 로그에서 다음을 확인:
```
Checking VPN connectivity: hostname=10.10.0.91, specifiedPort=-1, checkMethod=TCP
VPN will test default ports in order: 443, 80, 22, 8080
Attempting TCP connection to 10.10.0.91:443 (timeout: 5000ms)
VPN TCP connection success: hostname=10.10.0.91, port=443
```

어떤 포트가 성공하는지 확인하여 해당 포트가 VPN 서비스인지 확인 필요

## 즉시 조치 사항

1. **데이터베이스 확인**:
```sql
SELECT name, host, check_method, check_interval_sec, status, last_checked_at, last_status_change_at
FROM vpn_connections
WHERE name LIKE '%CARNM%';
```

2. **포트 테스트**:
```bash
# Windows PowerShell
Test-NetConnection -ComputerName 10.10.0.91 -Port 443
Test-NetConnection -ComputerName 10.10.0.91 -Port 80
Test-NetConnection -ComputerName 10.10.0.91 -Port 22
Test-NetConnection -ComputerName 10.10.0.91 -Port 8080
```

3. **워커 로그 확인**: 실제로 어떤 포트가 성공하는지 확인
