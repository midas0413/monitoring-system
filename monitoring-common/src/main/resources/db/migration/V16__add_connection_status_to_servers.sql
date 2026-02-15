-- 서버 연결상태 컬럼 추가 (UP/DOWN/UNKNOWN)
-- 알림 규칙 성공 시 UP, VPN 다운 또는 규칙 3회 연속 실패 시 DOWN
ALTER TABLE servers
  ADD COLUMN connection_status varchar(20) NOT NULL DEFAULT 'UNKNOWN';

COMMENT ON COLUMN servers.connection_status IS '연결상태: UP, DOWN, UNKNOWN. 규칙 성공 시 UP, VPN다운/규칙 3회 연속 실패 시 DOWN';

CREATE INDEX IF NOT EXISTS idx_servers_connection_status ON servers(connection_status);
