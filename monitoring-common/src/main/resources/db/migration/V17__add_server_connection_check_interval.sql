-- 서버별 연결상태 체크 주기(초). 0이면 주기 체크 미사용.
ALTER TABLE servers
  ADD COLUMN connection_check_interval_sec int NOT NULL DEFAULT 0;

-- 마지막 연결상태 체크 시각 (ping 테스트 수행 시점)
ALTER TABLE servers
  ADD COLUMN last_connection_check_at timestamptz NULL;

COMMENT ON COLUMN servers.connection_check_interval_sec IS '연결상태 ping 체크 주기(초). 0=미사용';
COMMENT ON COLUMN servers.last_connection_check_at IS '마지막 연결상태(ping) 체크 시각';
