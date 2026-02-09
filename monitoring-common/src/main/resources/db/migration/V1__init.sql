-- V1__init.sql (Check 중심: checks 등록 시 연결 정보 포함, V2/V3 통합)

-- 1) checks (서버/연결 정보 포함, type별 SHELL은 SSH / SQL은 DB만)
create table if not exists checks (
    id bigserial primary key,
    type varchar(20) not null,                     -- SHELL / SQL
    name varchar(100) not null,
    target_name varchar(100) not null,             -- 표시용 (ex: DEV_DBMS)

    host varchar(255) not null,

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

-- seed checks (개발용, 연결 정보 포함)
insert into checks (type, name, target_name, host, port, ssh_username, ssh_password, db_type, db_port, db_name, db_url, db_username, db_password, script, interval_sec, enabled, next_run_at, created_at)
values ('SQL', 'DB_PING', 'DEV_POSTGRE', '172.22.1.113', 22, 'root', 'Cnit52940*', 'postgresql', 5432, 'monitoring',
 'jdbc:postgresql://172.22.1.113:5432/monitoring', 'monitoring', 'monitoring', 'select count(*) from check_runs', 60, true, now(), now());

insert into checks (type, name, target_name, host, port, ssh_username, ssh_password, db_type, db_port, db_name, db_url, db_username, db_password, script, interval_sec, enabled, next_run_at, created_at)
values ('SQL', 'DB_TEST', 'DEV_ORACLE', '172.25.6.133', 22, 'root', 'growin2$6*', 'oracle', 1521, 'irmdb',
 'jdbc:oracle:thin:@172.25.6.133:1521:irmdb', 'NMKIRM', 'cc52940', 'select count(*) from TB_IRS001M', 60, true, now(), now());

insert into checks (type, name, target_name, host, port, ssh_username, ssh_password, script, interval_sec, enabled, next_run_at, created_at)
values ('SHELL', 'ECHO', 'TCS_DB', '172.22.1.113', 22, 'root', '2940*', 'echo date', 60, true, now(), now());

-- alert_rules seed
insert into alert_rules ("name", enabled, check_id, rule_type, threshold_num, threshold_len, pattern, channels, message_template, cooldown_sec, last_fired_at, created_at)
values ('[DB count] 설정치 초과', true, 1, 'OUTPUT_NUM_GT', 20, 300, '', 'KAKAO', '[{targetName}] check={checkName} outputLen={outputLen} threshold={threshold} status={status}', 300, null, now());

insert into alert_rules ("name", enabled, check_id, rule_type, threshold_num, threshold_len, pattern, channels, message_template, cooldown_sec, last_fired_at, created_at)
values ('[DB ping] output 길이 임계치 초과', true, 2, 'OUTPUT_LEN_GT', 200, 300, '', 'KAKAO', '[{targetName}] check={checkName} outputLen={outputLen} threshold={threshold} status={status}', 300, null, now());

-- alert_recipients seed
insert into alert_recipients ("name", enabled, channels, phone, email, kakao, created_at)
values ('Alex', true, 'KAKAO', '01053250413', 'hjkim@cnit21.com', '@grioom22', now());

insert into alert_recipients ("name", enabled, channels, phone, email, kakao, created_at)
values ('Zena', true, 'KAKAO', '01022529095', 'jjaeock108@cnit21.com', '@jjaeock', now());

-- alert_rule_recipient_links seed
insert into alert_rule_recipient_links (rule_id, recipient_id, enabled, created_at)
values (1, 1, true, now());

insert into alert_rule_recipient_links (rule_id, recipient_id, enabled, created_at)
values (1, 2, true, now());

-- 시퀀스 보정
SELECT setval('checks_id_seq', (SELECT COALESCE(MAX(id), 1) FROM checks));
SELECT setval('check_target_status_id_seq', (SELECT COALESCE(MAX(id), 1) FROM check_target_status));
SELECT setval('check_runs_id_seq', (SELECT COALESCE(MAX(id), 1) FROM check_runs));
SELECT setval('alert_rules_id_seq', (SELECT COALESCE(MAX(id), 1) FROM alert_rules));
SELECT setval('alert_recipients_id_seq', (SELECT COALESCE(MAX(id), 1) FROM alert_recipients));
SELECT setval('alert_rule_recipient_links_id_seq', (SELECT COALESCE(MAX(id), 1) FROM alert_rule_recipient_links));
