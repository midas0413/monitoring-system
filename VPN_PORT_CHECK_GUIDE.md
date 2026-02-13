# VPN 포트 체크 가이드

## 문제 상황

VPN 클라이언트가 실행되지 않은 상태에서도 VPN 상태가 UP으로 확인되는 경우가 있습니다.

## 원인 분석

현재 VPN 체크 로직은 **TCP 연결만** 확인합니다. 즉, 해당 IP의 어떤 포트든 TCP 연결이 성공하면 UP으로 판정됩니다.

### 기본 포트 확인 순서

포트가 지정되지 않은 경우 다음 순서로 확인합니다:
1. **443** (HTTPS) - 웹 서버가 실행 중이면 성공
2. **80** (HTTP) - 웹 서버가 실행 중이면 성공
3. **22** (SSH) - SSH 서버가 실행 중이면 성공
4. **8080** (HTTP 대체 포트) - 웹 서버가 실행 중이면 성공

**문제점**: 이 포트들은 VPN과 무관한 일반 서비스 포트입니다. 해당 IP에 웹 서버나 SSH 서버가 실행 중이면 VPN 클라이언트가 꺼져있어도 UP으로 판정됩니다.

## 해결 방법

### 1. VPN 전용 포트 지정 (권장)

VPN 설정에서 `host` 필드에 VPN 전용 포트를 명시하세요.

**예시**:
- OpenVPN: `vpn-server.example.com:1194` (UDP) 또는 `vpn-server.example.com:443` (TCP)
- IPSec: `vpn-server.example.com:500` (UDP)
- L2TP: `vpn-server.example.com:1701` (UDP)
- PPTP: `vpn-server.example.com:1723` (TCP)

**주의**: 현재 시스템은 TCP만 지원하므로 UDP 포트는 체크할 수 없습니다.

### 2. 로그 확인

워커 애플리케이션 로그에서 다음 메시지를 확인하세요:

```
# 성공한 포트 확인
VPN TCP connection success: hostname=xxx.xxx.xxx.xxx, port=80
WARNING: This port may not be a VPN service! (Could be web server, SSH, etc.)
VPN status set to UP based on port 80 connection, but this does not guarantee VPN service is running

# 체크되는 포트 목록 확인
VPN will test default ports in order: 443, 80, 22, 8080
```

### 3. 데이터베이스에서 설정 확인

```sql
SELECT 
    name,
    host,                    -- 포트가 지정되어 있는지 확인
    check_interval_sec,
    status,
    last_checked_at,
    last_status_change_at
FROM vpn_connections
WHERE name LIKE '%CARNM%';
```

### 4. 수동 포트 테스트

해당 IP의 어떤 포트가 열려있는지 확인:

```bash
# Windows PowerShell
Test-NetConnection -ComputerName <IP> -Port 80
Test-NetConnection -ComputerName <IP> -Port 443
Test-NetConnection -ComputerName <IP> -Port 22
Test-NetConnection -ComputerName <IP> -Port 8080

# Linux/Mac
nc -zv <IP> 80
nc -zv <IP> 443
nc -zv <IP> 22
nc -zv <IP> 8080
```

## 개선 사항

로그에 다음 정보가 추가되었습니다:

1. **성공한 포트 명시**: 어떤 포트로 UP 판정이 되었는지 로그에 기록
2. **경고 메시지**: UP 판정 시 VPN 서비스가 아닐 수 있음을 경고
3. **포트 테스트 순서**: 어떤 포트들을 테스트하는지 명시

## 권장 설정

1. **VPN 전용 포트 사용**: `host` 필드에 VPN 포트를 명시 (예: `vpn-server:1194`)
2. **포트가 지정되지 않은 경우**: 기본 포트(443, 80, 22, 8080)를 체크하지만, 이는 VPN 서비스가 아닐 수 있음을 인지
3. **로그 모니터링**: UP 판정 시 로그에서 실제로 어떤 포트가 성공했는지 확인

## 향후 개선 방향

1. **VPN 프로토콜별 포트 자동 감지**: VPN 타입에 따라 적절한 포트를 자동으로 선택
2. **UDP 포트 지원**: UDP 포트 체크 기능 추가 (현재는 TCP만 지원)
3. **VPN 서비스 검증**: TCP 연결 성공 후 실제 VPN 프로토콜 핸드셰이크 시도
