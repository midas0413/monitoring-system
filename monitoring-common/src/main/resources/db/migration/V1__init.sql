-- V1__init.sql (Check 중심: checks 등록 시 연결 정보 포함, V2/V3 통합)

-- 1) checks (서버/연결 정보 포함, type별 SHELL은 SSH / SQL은 DB만)
create table if not exists checks (
    id bigserial primary key,
    type varchar(20) not null,                     -- SHELL / SQL
    name varchar(100) not null,
    target_name varchar(100) not null,             -- 표시용 (ex: DEV_DBMS)

    host varchar(255) not null,
    timezone varchar(50) null,                     -- 서버 타임존 정보 (ex: Asia/Seoul, America/New_York)

    -- SHELL용
    port int null,                                 -- SSH port (기본 22)
    ssh_username varchar(100),
    ssh_password text,
    ssh_private_key_path text,

    -- SQL용
    db_type varchar(20),
    db_port int,
    db_name varchar(100),
    db_url text,
    db_username varchar(100),
    db_password text,

    script text not null,                          -- SHELL script 또는 SQL text
    interval_sec int not null default 60,
    enabled boolean not null default true,

    next_run_at timestamptz not null default now(),
    locked_until timestamptz null,
    locked_by varchar(100) null,

    last_run_at timestamptz null,
    last_status varchar(20) null,
    last_checked_at timestamptz null,
    last_error text null,
    status varchar(20) not null default 'UNKNOWN',

    created_at timestamptz not null default now()
);

create index if not exists idx_checks_type on checks(type);
create index if not exists idx_checks_enabled on checks(enabled);
create index if not exists idx_checks_next_run_at on checks(next_run_at);
create index if not exists idx_checks_locked_until on checks(locked_until);
create index if not exists idx_checks_due on checks(next_run_at) where enabled = true;
create index if not exists idx_checks_name on checks(name);

-- 2) check_target_status (history, check별 연결 상태)
create table if not exists check_target_status (
    id bigserial primary key,
    check_id bigint not null references checks(id) on delete cascade,

    status varchar(20) not null,
    latency_ms bigint null,
    checked_at timestamptz not null default now(),
    error_message varchar(500) null
);

create index if not exists idx_check_target_status_check_id_checked_at
    on check_target_status(check_id, checked_at desc);

-- 3) check_runs
create table if not exists check_runs (
    id bigserial primary key,
    check_id bigint not null references checks(id) on delete cascade,

    success boolean not null,

    started_at timestamptz not null default now(),
    finished_at timestamptz null,

    duration_ms bigint,
    output text,
    error_message varchar(1000)
);

create index if not exists idx_check_runs_check_id_started_at
    on check_runs(check_id, started_at desc);

-- 4) alert_rules (server_id 제거, check_id로 스코프)
create table if not exists alert_rules (
    id bigserial primary key,
    name varchar(100) not null,
    enabled boolean not null default true,

    check_id bigint null references checks(id) on delete cascade,

    rule_type varchar(40) not null,
    threshold_num double precision null,
    threshold_len int null,
    pattern text null,

    channels varchar(100) null,
    message_template text not null,

    cooldown_sec int not null default 300,
    last_fired_at timestamptz null,

    created_at timestamptz not null default now()
);

create index if not exists idx_alert_rules_enabled on alert_rules(enabled);
create index if not exists idx_alert_rules_check_id on alert_rules(check_id);

-- 5) alert_recipients
create table if not exists alert_recipients (
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

-- 6) alert_rule_recipient_links
create table if not exists alert_rule_recipient_links (
    id bigserial primary key,
    rule_id bigint not null references alert_rules(id) on delete cascade,
    recipient_id bigint not null references alert_recipients(id) on delete cascade,

    enabled boolean not null default true,
    created_at timestamptz not null default now(),

    unique (rule_id, recipient_id)
);

create index if not exists idx_arrl_rule_id on alert_rule_recipient_links(rule_id);
create index if not exists idx_arrl_enabled on alert_rule_recipient_links(enabled);

-- 7) notification_outbox
create table if not exists notification_outbox (
    id bigserial primary key,

    status varchar(20) not null default 'PENDING',
    channel varchar(20) not null,

    rule_id bigint null references alert_rules(id) on delete set null,
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

-- 8) timezone_codes (타임존 코드 테이블)
create table if not exists timezone_codes (
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

-- 주요 타임존 데이터 삽입
insert into timezone_codes (timezone_id, display_name, offset_hours, offset_minutes, description, display_order) values
-- 아시아/태평양
('Asia/Seoul', '한국 표준시 (KST)', 9, 0, '대한민국, 일본', 1),
('Asia/Tokyo', '일본 표준시 (JST)', 9, 0, '일본', 2),
('Asia/Shanghai', '중국 표준시 (CST)', 8, 0, '중국, 대만, 홍콩', 3),
('Asia/Hong_Kong', '홍콩 표준시 (HKT)', 8, 0, '홍콩', 4),
('Asia/Singapore', '싱가포르 표준시 (SGT)', 8, 0, '싱가포르, 말레이시아', 5),
('Asia/Bangkok', '태국 표준시 (ICT)', 7, 0, '태국, 베트남', 6),
('Asia/Jakarta', '인도네시아 서부 표준시 (WIB)', 7, 0, '인도네시아 서부', 7),
('Asia/Manila', '필리핀 표준시 (PHT)', 8, 0, '필리핀', 8),
('Asia/Kolkata', '인도 표준시 (IST)', 5, 30, '인도', 9),
('Asia/Dubai', '아랍에미리트 표준시 (GST)', 4, 0, 'UAE, 오만', 10),

-- 유럽
('Europe/London', '영국 표준시 (GMT/BST)', 0, 0, '영국, 아일랜드', 20),
('Europe/Paris', '중앙유럽 표준시 (CET/CEST)', 1, 0, '프랑스, 독일, 이탈리아, 스페인', 21),
('Europe/Berlin', '독일 표준시 (CET/CEST)', 1, 0, '독일', 22),
('Europe/Rome', '이탈리아 표준시 (CET/CEST)', 1, 0, '이탈리아', 23),
('Europe/Madrid', '스페인 표준시 (CET/CEST)', 1, 0, '스페인', 24),
('Europe/Amsterdam', '네덜란드 표준시 (CET/CEST)', 1, 0, '네덜란드', 25),
('Europe/Stockholm', '스웨덴 표준시 (CET/CEST)', 1, 0, '스웨덴', 26),
('Europe/Moscow', '모스크바 표준시 (MSK)', 3, 0, '러시아 서부', 27),
('Europe/Athens', '그리스 표준시 (EET/EEST)', 2, 0, '그리스', 28),
('Europe/Istanbul', '터키 표준시 (TRT)', 3, 0, '터키', 29),

-- 아메리카
('America/New_York', '미국 동부 표준시 (EST/EDT)', -5, 0, '미국 동부', 40),
('America/Chicago', '미국 중부 표준시 (CST/CDT)', -6, 0, '미국 중부', 41),
('America/Denver', '미국 산지 표준시 (MST/MDT)', -7, 0, '미국 산지', 42),
('America/Los_Angeles', '미국 태평양 표준시 (PST/PDT)', -8, 0, '미국 서부', 43),
('America/Toronto', '캐나다 동부 표준시 (EST/EDT)', -5, 0, '캐나다 동부', 44),
('America/Vancouver', '캐나다 태평양 표준시 (PST/PDT)', -8, 0, '캐나다 서부', 45),
('America/Mexico_City', '멕시코 표준시 (CST)', -6, 0, '멕시코', 46),
('America/Sao_Paulo', '브라질 표준시 (BRT)', -3, 0, '브라질', 47),
('America/Buenos_Aires', '아르헨티나 표준시 (ART)', -3, 0, '아르헨티나', 48),

-- 기타
('UTC', '협정 세계시 (UTC)', 0, 0, 'UTC', 100),
('Australia/Sydney', '호주 동부 표준시 (AEST/AEDT)', 10, 0, '호주 동부', 60),
('Australia/Melbourne', '호주 동부 표준시 (AEST/AEDT)', 10, 0, '호주 동부', 61),
('Pacific/Auckland', '뉴질랜드 표준시 (NZST/NZDT)', 12, 0, '뉴질랜드', 70);




-- 시퀀스 보정
SELECT setval('checks_id_seq', (SELECT COALESCE(MAX(id), 1) FROM checks));
SELECT setval('check_target_status_id_seq', (SELECT COALESCE(MAX(id), 1) FROM check_target_status));
SELECT setval('check_runs_id_seq', (SELECT COALESCE(MAX(id), 1) FROM check_runs));
SELECT setval('alert_rules_id_seq', (SELECT COALESCE(MAX(id), 1) FROM alert_rules));
SELECT setval('alert_recipients_id_seq', (SELECT COALESCE(MAX(id), 1) FROM alert_recipients));
SELECT setval('alert_rule_recipient_links_id_seq', (SELECT COALESCE(MAX(id), 1) FROM alert_rule_recipient_links));
