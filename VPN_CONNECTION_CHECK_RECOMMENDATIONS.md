# VPN 연결 상태 체크 방법 제안

## 개요
타임존이 한국이 아닌 서버들은 VPN을 통해 연결되기 때문에, VPN 연결 상태를 모니터링하는 것이 중요합니다.

## 추천 방법 (우선순위 순)

### 1. **VPN 게이트웨이 IP Ping 체크** (가장 간단하고 효과적)
- **방법**: VPN 게이트웨이 IP 주소에 ping을 보내서 응답 여부 확인
- **장점**: 
  - 구현이 간단함
  - VPN 연결 상태를 직접 확인 가능
  - 네트워크 레벨에서 체크하므로 정확함
- **단점**: 
  - VPN 게이트웨이 IP를 알아야 함
  - 일부 VPN은 ICMP를 차단할 수 있음
- **구현 예시**:
  ```bash
  # SHELL 체크로 구현
  ping -c 3 -W 2 <VPN_GATEWAY_IP>
  # 성공 시: exit code 0, 실패 시: exit code 1
  ```

### 2. **VPN 터널 인터페이스 확인** (Linux/Unix 환경)
- **방법**: VPN 터널 인터페이스(예: `tun0`, `ppp0`, `wg0`) 존재 여부 확인
- **장점**: 
  - VPN 소프트웨어와 무관하게 작동
  - 인터페이스가 있으면 VPN이 연결된 것으로 간주
- **단점**: 
  - SSH 접근이 필요함
  - VPN 종류에 따라 인터페이스 이름이 다를 수 있음
- **구현 예시**:
  ```bash
  # OpenVPN (tun0)
  ip link show tun0 > /dev/null 2>&1 && echo "UP" || echo "DOWN"
  
  # WireGuard (wg0)
  ip link show wg0 > /dev/null 2>&1 && echo "UP" || echo "DOWN"
  
  # 일반적인 VPN 인터페이스 확인
  ip link | grep -E "tun|ppp|wg" | grep -q "state UP" && echo "UP" || echo "DOWN"
  ```

### 3. **VPN 프로세스 및 서비스 상태 확인**
- **방법**: VPN 클라이언트 프로세스나 서비스 상태 확인
- **장점**: 
  - VPN 소프트웨어 레벨에서 확인 가능
- **단점**: 
  - VPN 종류에 따라 다름
  - 프로세스가 실행 중이어도 연결이 끊어질 수 있음
- **구현 예시**:
  ```bash
  # OpenVPN 프로세스 확인
  pgrep -x openvpn > /dev/null && echo "UP" || echo "DOWN"
  
  # systemd 서비스 확인 (예: strongswan)
  systemctl is-active strongswan > /dev/null && echo "UP" || echo "DOWN"
  ```

### 4. **특정 포트 연결 체크** (VPN을 통한 서비스 접근 확인)
- **방법**: VPN을 통해 접근 가능한 내부 서비스의 포트에 연결 시도
- **장점**: 
  - 실제 서비스 접근 가능 여부를 확인
  - VPN이 연결되어 있어도 라우팅 문제를 감지 가능
- **단점**: 
  - 대상 서비스가 실행 중이어야 함
  - 방화벽 설정에 따라 차단될 수 있음
- **구현 예시**:
  ```bash
  # SSH 포트 체크
  timeout 3 bash -c "</dev/tcp/<INTERNAL_SERVER_IP>/22" && echo "UP" || echo "DOWN"
  
  # HTTP 포트 체크
  curl -s --connect-timeout 3 http://<INTERNAL_SERVER_IP>:80 > /dev/null && echo "UP" || echo "DOWN"
  ```

### 5. **VPN 로그 확인** (고급)
- **방법**: VPN 로그 파일에서 연결 상태 확인
- **장점**: 
  - 상세한 연결 정보 확인 가능
- **단점**: 
  - 로그 파일 위치와 형식이 VPN마다 다름
  - 로그 파싱이 복잡할 수 있음
- **구현 예시**:
  ```bash
  # OpenVPN 로그에서 최근 연결 확인
  tail -n 100 /var/log/openvpn.log | grep -q "Initialization Sequence Completed" && echo "UP" || echo "DOWN"
  ```

## 권장 구현 방안

### 옵션 A: VPN 게이트웨이 Ping 체크 (추천)
1. **VPN 서버별로 게이트웨이 IP를 설정**
   - Check 엔티티에 `vpnGatewayIp` 필드 추가 (선택적)
   - 또는 Rule 설정에서 VPN 게이트웨이 IP 지정

2. **VPN 체크용 Rule Type 추가**
   - `VPN_CONNECTION` 타입 추가
   - Ping 체크를 수행하는 스크립트 실행

3. **스크립트 예시**:
   ```bash
   #!/bin/bash
   VPN_GATEWAY=${VPN_GATEWAY_IP}
   if ping -c 3 -W 2 "$VPN_GATEWAY" > /dev/null 2>&1; then
     echo "VPN_CONNECTED"
     exit 0
   else
     echo "VPN_DISCONNECTED"
     exit 1
   fi
   ```

### 옵션 B: VPN 인터페이스 확인 (Linux 서버용)
1. **SSH를 통해 VPN 인터페이스 확인**
   - 기존 SHELL 체크 활용
   - VPN 인터페이스 이름을 Rule 설정에 추가

2. **스크립트 예시**:
   ```bash
   #!/bin/bash
   VPN_INTERFACE=${VPN_INTERFACE_NAME:-tun0}
   if ip link show "$VPN_INTERFACE" 2>/dev/null | grep -q "state UP"; then
     echo "VPN_UP"
     exit 0
   else
     echo "VPN_DOWN"
     exit 1
   fi
   ```

### 옵션 C: 하이브리드 방식 (가장 안정적)
1. **여러 방법을 조합**
   - VPN 게이트웨이 Ping + 인터페이스 확인
   - 둘 중 하나라도 실패하면 VPN DOWN으로 판단

2. **스크립트 예시**:
   ```bash
   #!/bin/bash
   VPN_GATEWAY=${VPN_GATEWAY_IP}
   VPN_INTERFACE=${VPN_INTERFACE_NAME:-tun0}
   
   # 방법 1: Ping 체크
   PING_OK=false
   if ping -c 2 -W 2 "$VPN_GATEWAY" > /dev/null 2>&1; then
     PING_OK=true
   fi
   
   # 방법 2: 인터페이스 체크
   INTERFACE_OK=false
   if ip link show "$VPN_INTERFACE" 2>/dev/null | grep -q "state UP"; then
     INTERFACE_OK=true
   fi
   
   # 둘 다 성공해야 VPN 연결됨
   if [ "$PING_OK" = true ] && [ "$INTERFACE_OK" = true ]; then
     echo "VPN_CONNECTED"
     exit 0
   else
     echo "VPN_DISCONNECTED (ping=$PING_OK, interface=$INTERFACE_OK)"
     exit 1
   fi
   ```

## 구현 제안

### 1단계: Check 엔티티에 VPN 관련 필드 추가
```sql
ALTER TABLE checks ADD COLUMN IF NOT EXISTS vpn_gateway_ip VARCHAR(50) NULL;
ALTER TABLE checks ADD COLUMN IF NOT EXISTS vpn_interface_name VARCHAR(50) NULL;
ALTER TABLE checks ADD COLUMN IF NOT EXISTS requires_vpn BOOLEAN NOT NULL DEFAULT false;
```

### 2단계: VPN 체크용 Rule Type 추가
- `AlertRuleType`에 `VPN_DISCONNECTED` 추가
- VPN 연결이 끊어지면 알림 발생

### 3단계: VPN 체크 스크립트 템플릿 제공
- Rule 생성 시 VPN 체크용 스크립트 템플릿 제공
- 사용자가 VPN 게이트웨이 IP나 인터페이스 이름만 입력하면 됨

## 참고사항
- VPN 종류별로 체크 방법이 다를 수 있으므로, 서버 환경에 맞는 방법 선택
- VPN 연결이 불안정한 경우, 여러 번 재시도하거나 타임아웃 설정 필요
- VPN 체크 실패 시 즉시 알림이 발생하도록 Cooldown 시간을 짧게 설정 권장
