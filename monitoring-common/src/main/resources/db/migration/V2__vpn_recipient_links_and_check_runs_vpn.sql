-- VPN 수신자 연결 테이블 (VPN별 알림 수신자)
CREATE TABLE IF NOT EXISTS vpn_recipient_links (
    id BIGSERIAL PRIMARY KEY,
    vpn_id BIGINT NOT NULL REFERENCES vpn_connections(id) ON DELETE CASCADE,
    recipient_id BIGINT NOT NULL REFERENCES alert_recipients(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (vpn_id, recipient_id)
);

CREATE INDEX IF NOT EXISTS idx_vpn_recipient_links_vpn_id ON vpn_recipient_links(vpn_id);
CREATE INDEX IF NOT EXISTS idx_vpn_recipient_links_recipient_id ON vpn_recipient_links(recipient_id);

-- check_runs: VPN 알림 내역 저장용 (monitoring_rule_id nullable, vpn_id 추가)
ALTER TABLE check_runs ALTER COLUMN monitoring_rule_id DROP NOT NULL;
ALTER TABLE check_runs ADD COLUMN IF NOT EXISTS vpn_id BIGINT REFERENCES vpn_connections(id) ON DELETE SET NULL;

ALTER TABLE check_runs ADD CONSTRAINT chk_check_runs_rule_or_vpn
    CHECK (monitoring_rule_id IS NOT NULL OR vpn_id IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_check_runs_vpn_id_started_at ON check_runs(vpn_id, started_at DESC);
