# CARNM_VPN UP/DOWN 반복 원인 분석

## 현재 VPN 체크 방식

### 1. 체크 방법
- **Ping (ICMP)**: 사용하지 않음 (방화벽에서 차단될 수 있어 신뢰할 수 없음)
- **TCP 연결 테스트만 사용**: `Socket.connect()` 메서드로 연결 시도

### 2. 포트 확인 순서
코드 위치: `VpnCheckService.java`의 `checkConnectivity()` 메서드

```java
// host에 포트가 지정되어 있으면 (host:port 형식) 해당 포트만 시도
if (specifiedPort > 0) {
    portsToTest = new int[]{specifiedPort};
} else {
    // 기본 포트들 시도: 443, 80, 22, 8080 (HTTPS 우선)
    portsToTest = new int[]{443, 80, 22, 8080};
}
```

**포트 확인 순서**: 443 → 80 → 22 → 8080

### 3. 연결 타임아웃
- **타임아웃**: 5초 (`CONNECTION_TIMEOUT_MS = 5000`)
- 각 포트마다 최대 5초 대기

### 4. 상태 판정 로직

```java
for (int port : portsToTest) {
    try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress(hostname, port), CONNECTION_TIMEOUT_MS);
        log.info("VPN TCP connection success: hostname={}, port={}", hostname, port);
        return ServerStatus.UP;  // 하나라도 성공하면 즉시 UP 반환
    } catch (java.net.ConnectException e) {
        // 연결 거부 - 포트는 열려있지만 서비스가 없거나 거부
        log.debug("VPN TCP connection refused: hostname={}, port={}", hostname, port);
        anyPortReachable = true; // 포트는 열려있음
    } catch (java.net.SocketTimeoutException e) {
        // 타임아웃 - 연결 불가
        log.debug("VPN TCP connection timeout: hostname={}, port={}", hostname, port);
    }
}
// 모든 포트 테스트 실패 시 DOWN 반환
return ServerStatus.DOWN;
```

## 문제 원인 분석

### 가능한 원인들

1. **포트 불안정성**
   - 80번 포트가 간헐적으로 응답하지 않거나 연결이 불안정한 경우
   - 네트워크 지연으로 인해 5초 타임아웃 내에 응답하지 못하는 경우

2. **ConnectException vs SocketTimeoutException 혼재**
   - `ConnectException`: 포트는 열려있지만 연결 거부 (서비스가 일시적으로 중단)
   - `SocketTimeoutException`: 포트 자체가 닫혀있거나 방화벽 차단
   - 두 경우가 번갈아 발생하면 UP/DOWN이 반복될 수 있음

3. **체크 주기와 네트워크 상태 불일치**
   - VPN 체크 주기(`check_interval_sec`)가 너무 짧아서 일시적인 네트워크 지연을 감지
   - 네트워크가 불안정한 경우 짧은 주기로 체크하면 UP/DOWN이 반복될 수 있음

## 진단 방법

### 1. 데이터베이스에서 CARNM_VPN 설정 확인

```sql
SELECT 
    id,
    name,
    host,                    -- 포트가 지정되어 있는지 확인 (예: host:80)
    check_interval_sec,       -- 체크 주기 확인
    enabled,
    status,
    last_checked_at,
    last_status_change_at
FROM vpn_connections
WHERE name LIKE '%CARNM%' OR name LIKE '%carnm%';
```

### 2. 애플리케이션 로그 확인

워커 애플리케이션 로그에서 다음 메시지들을 확인:

```
# VPN 체크 시작
Checking VPN: id={}, name={}, host={}, lastChecked={}

# TCP 연결 성공 (UP)
VPN TCP connection success: hostname={}, port={}

# TCP 연결 거부 (포트는 열려있지만 연결 거부)
VPN TCP connection refused: hostname={}, port={}

# TCP 연결 타임아웃 (포트 연결 불가)
VPN TCP connection timeout: hostname={}, port={}

# VPN 상태 변경
VPN status changed: id={}, name={}, {} -> {}

# VPN 연결 실패 (DOWN)
VPN connection failed: hostname={}, all ports unreachable
```

### 3. 어떤 포트가 체크되고 있는지 확인

로그에서 다음 패턴을 확인:
- `VPN TCP connection success: hostname=CARNM_VPN_HOST, port=80` → 80번 포트로 UP 판정
- `VPN TCP connection refused: hostname=CARNM_VPN_HOST, port=80` → 80번 포트는 열려있지만 연결 거부
- `VPN TCP connection timeout: hostname=CARNM_VPN_HOST, port=80` → 80번 포트 타임아웃

## 해결 방안

### 1. 포트 명시적 지정
CARNM_VPN의 `host` 필드에 안정적인 포트를 명시:
- 예: `CARNM_VPN_HOST:80` 또는 `CARNM_VPN_HOST:443`
- 이렇게 하면 해당 포트만 체크하므로 불필요한 포트 시도 제거

### 2. 체크 주기 조정
`check_interval_sec` 값을 늘려서 너무 자주 체크하지 않도록 설정:
- 기본값: 60초
- 권장: 120초 이상 (네트워크가 불안정한 경우)

### 3. 타임아웃 시간 조정
`CONNECTION_TIMEOUT_MS` 값을 늘려서 네트워크 지연에 여유를 둠:
- 현재: 5000ms (5초)
- 권장: 10000ms (10초) - 단, 모든 포트를 시도하면 최대 40초 소요

### 4. 재시도 로직 추가
연결 실패 시 일정 횟수만큼 재시도하여 일시적인 네트워크 문제를 필터링

### 5. 로그 레벨 조정
`log.debug()`로 설정된 메시지들을 `log.info()`로 변경하여 실제 체크 과정을 더 자세히 확인

## 다음 단계

1. **데이터베이스 확인**: CARNM_VPN의 `host`와 `check_interval_sec` 값 확인
2. **로그 분석**: 워커 애플리케이션 로그에서 실제 체크 과정 확인
3. **네트워크 테스트**: 수동으로 해당 호스트의 포트 연결 테스트
4. **설정 조정**: 위의 해결 방안 중 적절한 방법 적용
