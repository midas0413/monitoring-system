-- CARNM_VPN 설정 확인
SELECT 
    id,
    name,
    host,
    check_interval_sec,
    enabled,
    status,
    last_checked_at,
    last_status_change_at,
    description
FROM vpn_connections
WHERE name LIKE '%CARNM%' OR name LIKE '%carnm%'
ORDER BY name;

-- 최근 VPN 상태 변경 이력 확인 (체크 실행 로그는 check_runs 테이블에 있을 수 있음)
-- VPN 체크는 별도 로그에 기록되므로 애플리케이션 로그를 확인해야 함
