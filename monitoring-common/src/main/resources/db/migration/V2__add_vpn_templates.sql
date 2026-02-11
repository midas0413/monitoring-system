-- V2__add_vpn_templates.sql
-- VPN 알림 메시지 템플릿 테이블 추가

create table vpn_notification_templates (
    id bigserial primary key,
    vpn_id bigint not null references vpn_connections(id) on delete cascade,
    name varchar(100) not null,                      -- 템플릿 이름
    title_template text not null,                    -- 제목 템플릿
    body_template text not null,                     -- 본문 템플릿
    enabled boolean not null default true,
    description varchar(500) null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (vpn_id, name)
);

create index if not exists idx_vpn_templates_vpn_id on vpn_notification_templates(vpn_id);
create index if not exists idx_vpn_templates_enabled on vpn_notification_templates(enabled);

-- 템플릿 변수 설명:
-- $${vpnName} - VPN 이름
-- $${vpnHost} - VPN 호스트
-- $${oldStatus} - 이전 상태 (UP/DOWN/UNKNOWN)
-- $${newStatus} - 현재 상태 (UP/DOWN/UNKNOWN)
-- $${changeTime} - 상태 변경 시간 (yyyy-MM-dd HH:mm:ss)
