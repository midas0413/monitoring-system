-- VPN 수신자 연결 및 check_runs.vpn_id (DB가 이미 V4인 경우 적용)
-- V2가 스킵된 환경에서 스키마를 엔티티에 맞추기 위한 마이그레이션

-- 1) vpn_recipient_links 테이블 (없을 때만 생성)
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

-- 2) check_runs: monitoring_rule_id nullable, vpn_id 컬럼 추가
ALTER TABLE check_runs ALTER COLUMN monitoring_rule_id DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'check_runs' AND column_name = 'vpn_id'
    ) THEN
        ALTER TABLE check_runs ADD COLUMN vpn_id BIGINT REFERENCES vpn_connections(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 3) 제약조건 (없을 때만 추가)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_check_runs_rule_or_vpn'
    ) THEN
        ALTER TABLE check_runs ADD CONSTRAINT chk_check_runs_rule_or_vpn
            CHECK (monitoring_rule_id IS NOT NULL OR vpn_id IS NOT NULL);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_check_runs_vpn_id_started_at ON check_runs(vpn_id, started_at DESC);
