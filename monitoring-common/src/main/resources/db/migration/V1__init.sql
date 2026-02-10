-- V1__init.sql (서버 기반 모니터링 시스템 초기 스키마)
-- DB 초기화용: 기존 테이블 삭제 후 재생성

-- 기존 테이블 삭제 (초기화용)
DROP TABLE IF EXISTS notification_outbox CASCADE;
DROP TABLE IF EXISTS alert_rule_recipient_links CASCADE;
DROP TABLE IF EXISTS alert_recipients CASCADE;
DROP TABLE IF EXISTS check_runs CASCADE;
DROP TABLE IF EXISTS monitoring_rules CASCADE;
DROP TABLE IF EXISTS server_vpn_links CASCADE;
DROP TABLE IF EXISTS servers CASCADE;
DROP TABLE IF EXISTS vpn_connections CASCADE;
DROP TABLE IF EXISTS notification_settings CASCADE;
DROP TABLE IF EXISTS system_codes CASCADE;
DROP TABLE IF EXISTS timezone_codes CASCADE;

-- 1) servers (서버 정보 관리)
create table servers (
    id bigserial primary key,
    name varchar(100) not null unique,              -- 서버명 (ex: DEV_DBMS, PROD_WEB)
    host varchar(255) not null,                     -- 호스트 (IP 또는 도메인)
    timezone varchar(50) not null,                 -- 타임존 (timezone_codes 참조)
    server_purpose varchar(20) not null,           -- 서버용도: WEB, WAS, DBMS, APP, ETC
    enabled boolean not null default true,
    description varchar(500) null,
    
    -- SSH 연결 정보 (SHELL, LOGS, DISK_SPACE 모니터링용)
    ssh_port int null default 22,                  -- SSH 포트
    ssh_username varchar(100) null,                -- SSH 사용자명
    ssh_password text null,                        -- SSH 비밀번호
    ssh_private_key_path text null,                -- SSH Private Key 경로
    
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_servers_enabled on servers(enabled);
create index if not exists idx_servers_name on servers(name);

-- 2) vpn_connections (VPN 연결 정보)
create table vpn_connections (
    id bigserial primary key,
    name varchar(100) not null unique,              -- VPN명 (ex: DEV_VPN, PROD_VPN)
    host varchar(255) not null,                     -- VPN 호스트 (IP 또는 URL)
    check_interval_sec int not null default 60,     -- VPN 상태 체크 주기 (초)
    enabled boolean not null default true,
    status varchar(20) not null default 'UNKNOWN', -- UP, DOWN, UNKNOWN
    last_checked_at timestamptz null,
    last_status_change_at timestamptz null,
    description varchar(500) null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_vpn_connections_enabled on vpn_connections(enabled);
create index if not exists idx_vpn_connections_status on vpn_connections(status);

-- 3) server_vpn_links (서버와 VPN 연결 관계)
create table server_vpn_links (
    id bigserial primary key,
    server_id bigint not null references servers(id) on delete cascade,
    vpn_id bigint not null references vpn_connections(id) on delete cascade,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    unique (server_id, vpn_id)
);

create index if not exists idx_server_vpn_links_server_id on server_vpn_links(server_id);
create index if not exists idx_server_vpn_links_vpn_id on server_vpn_links(vpn_id);

-- 4) monitoring_rules (모니터링 룰 - checks와 alert_rules 통합)
create table monitoring_rules (
    id bigserial primary key,
    name varchar(100) not null,                     -- 룰 이름
    server_id bigint not null references servers(id) on delete cascade,
    
    -- 모니터링 타입: SHELL, DB, LOGS, DISK_SPACE
    monitoring_type varchar(20) not null,
    
    -- 공통 필드
    enabled boolean not null default true,
    interval_sec int not null default 60,
    
    -- SSH 공통 정보 (SHELL, LOGS, DISK_SPACE용)
    ssh_port int null,                             -- SSH 포트 (기본 22)
    ssh_username varchar(100) null,
    ssh_password text null,
    ssh_private_key_path text null,
    
    -- DB 타입 전용 필드
    db_type varchar(20) null,                      -- oracle, postgresql, mysql 등
    db_port int null,
    db_name varchar(100) null,
    db_url text null,
    db_username varchar(100) null,
    db_password text null,
    
    -- SHELL 타입 전용
    shell_script text null,                        -- Shell 스크립트
    
    -- LOGS 타입 전용
    log_file_path text null,                       -- 로그 파일 경로
    include_keywords text null,                    -- 포함 단어 (쉼표 구분: ERROR, FAIL, CRITICAL)
    exclude_keywords text null,                     -- 제외 단어 (쉼표 구분)
    
    -- DISK_SPACE 타입 전용
    disk_path varchar(500) null,                   -- 디스크 경로 (ex: /, /var, /home)
    
    -- 알림 규칙
    alert_operator varchar(40) not null,           -- RUN_FAILED, OUTPUT_NUM_GT, OUTPUT_NUM_LT, OUTPUT_CONTAINS, OUTPUT_NOT_CONTAINS, OUTPUT_MATCHES
    threshold_num double precision null,            -- 숫자 임계값
    threshold_len int null,                         -- 길이 임계값
    pattern text null,                              -- 정규식 패턴
    
    -- 알림 설정
    channels varchar(100) null,                    -- 알림 채널 (SMS,EMAIL,KAKAO)
    message_template text not null,                 -- 메시지 템플릿
    cooldown_sec int not null default 300,         -- 쿨다운 시간 (초)
    
    -- 실행 관리
    next_run_at timestamptz not null default now(),
    locked_until timestamptz null,
    locked_by varchar(100) null,
    last_run_at timestamptz null,
    last_status varchar(20) null,
    last_fired_at timestamptz null,
    
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_monitoring_rules_server_id on monitoring_rules(server_id);
create index if not exists idx_monitoring_rules_enabled on monitoring_rules(enabled);
create index if not exists idx_monitoring_rules_type on monitoring_rules(monitoring_type);
create index if not exists idx_monitoring_rules_next_run_at on monitoring_rules(next_run_at);
create index if not exists idx_monitoring_rules_due on monitoring_rules(next_run_at) where enabled = true;

-- 5) check_runs (monitoring_rule_id 참조)
create table check_runs (
    id bigserial primary key,
    monitoring_rule_id bigint not null references monitoring_rules(id) on delete cascade,

    success boolean not null,

    started_at timestamptz not null default now(),
    finished_at timestamptz null,

    duration_ms bigint,
    output text,
    error_message varchar(1000)
);

create index if not exists idx_check_runs_monitoring_rule_id_started_at
    on check_runs(monitoring_rule_id, started_at desc);

-- 6) alert_recipients
create table alert_recipients (
    id bigserial primary key,
    name varchar(100) not null,
    enabled boolean not null default true,

    channels varchar(100) not null,

    phone varchar(50) null,
    email varchar(200) null,
    kakao varchar(200) null,

    created_at timestamptz not null default now()
);

create index if not exists idx_alert_recipients_enabled on alert_recipients(enabled);

-- 7) alert_rule_recipient_links (monitoring_rule_id 참조로 사용)
create table alert_rule_recipient_links (
    id bigserial primary key,
    rule_id bigint not null references monitoring_rules(id) on delete cascade,
    recipient_id bigint not null references alert_recipients(id) on delete cascade,

    enabled boolean not null default true,
    created_at timestamptz not null default now(),

    unique (rule_id, recipient_id)
);

create index if not exists idx_arrl_rule_id on alert_rule_recipient_links(rule_id);
create index if not exists idx_arrl_enabled on alert_rule_recipient_links(enabled);

-- 8) notification_outbox (monitoring_rule_id 참조)
create table notification_outbox (
    id bigserial primary key,

    status varchar(20) not null default 'PENDING',
    channel varchar(20) not null,

    monitoring_rule_id bigint null references monitoring_rules(id) on delete set null,
    check_run_id bigint null references check_runs(id) on delete set null,

    to_addr varchar(200) not null,
    title varchar(200) null,
    body text not null,

    attempt int not null default 0,
    max_attempt int not null default 5,
    last_error text null,
    next_attempt_at timestamptz not null default now(),

    processing_by varchar(100) null,
    processing_until timestamptz null,

    created_at timestamptz not null default now(),
    sent_at timestamptz null
);

create index if not exists idx_outbox_due on notification_outbox(status, next_attempt_at, id);
create index if not exists idx_outbox_processing_until on notification_outbox(status, processing_until);
create index if not exists idx_outbox_monitoring_rule_id on notification_outbox(monitoring_rule_id, created_at);

-- 9) notification_settings (알림 설정 - 알리고 정보 등)
create table notification_settings (
    id bigserial primary key,
    name varchar(100) not null unique,              -- 설정 이름 (ex: ALIGO_MAIN)
    provider varchar(50) not null,                 -- 알림 제공자: ALIGO, SMTP, SLACK 등
    enabled boolean not null default true,
    
    -- Aligo 설정
    aligo_api_key varchar(200) null,
    aligo_user_id varchar(100) null,
    aligo_sender varchar(50) null,
    aligo_sender_key varchar(200) null,
    aligo_template_code varchar(100) null,
    aligo_test_mode varchar(1) null default 'N',  -- Y: 테스트, N: 실제 전송
    
    -- SMTP 설정
    smtp_host varchar(255) null,
    smtp_port int null,
    smtp_username varchar(200) null,
    smtp_password text null,
    smtp_from_email varchar(200) null,
    
    -- 기타 설정 (JSON 형식)
    extra_config text null,
    
    description varchar(500) null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_notification_settings_enabled on notification_settings(enabled);
create index if not exists idx_notification_settings_provider on notification_settings(provider);

-- 10) timezone_codes (타임존 코드 테이블)
create table timezone_codes (
    id bigserial primary key,
    timezone_id varchar(100) not null unique,
    display_name varchar(200) not null,
    offset_hours integer not null,  -- UTC 기준 시차 (시간)
    offset_minutes integer not null default 0,  -- UTC 기준 시차 (분)
    description varchar(500) null,
    enabled boolean not null default true,
    display_order integer not null default 0,
    created_at timestamptz not null default now()
);

create index if not exists idx_timezone_codes_enabled on timezone_codes(enabled, display_order);

-- 11) system_codes (시스템 코드 관리 - 타임존 등 확장 가능)
create table system_codes (
    id bigserial primary key,
    code_type varchar(50) not null,                -- 코드 타입: SERVER_PURPOSE, MONITORING_TYPE, ALERT_OPERATOR 등
    code_value varchar(100) not null,             -- 코드 값
    code_label varchar(200) not null,            -- 코드 라벨 (표시명)
    display_order int not null default 0,
    enabled boolean not null default true,
    description varchar(500) null,
    created_at timestamptz not null default now(),
    unique (code_type, code_value)
);

create index if not exists idx_system_codes_type on system_codes(code_type, enabled, display_order);