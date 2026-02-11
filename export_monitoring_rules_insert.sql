-- monitoring_rules 테이블 데이터를 INSERT 쿼리로 생성하는 스크립트
-- DB 클라이언트에서 실행하여 INSERT 문을 생성합니다.
-- 생성된 INSERT 문은 결과를 복사하여 사용하거나, 파일로 저장할 수 있습니다.
--
-- 주의사항:
-- 1. message_template의 ${} 표시는 quote_literal() 함수로 자동 이스케이프되어 텍스트로 인식됩니다.
-- 2. PostgreSQL의 quote_literal() 함수는 작은따옴표(')를 자동으로 이스케이프('') 처리합니다.
-- 3. NULL 값은 'NULL' 문자열로 출력되며, 실제 NULL로 삽입됩니다.
-- 4. timestamp 타입은 문자열로 변환되어 삽입됩니다.

SELECT 
    'INSERT INTO monitoring_rules (' ||
    'id, name, server_id, monitoring_type, enabled, interval_sec, ' ||
    'ssh_port, ssh_username, ssh_password, ssh_private_key_path, ' ||
    'db_type, db_port, db_name, db_url, db_username, db_password, ' ||
    'shell_script, log_file_path, include_keywords, exclude_keywords, ' ||
    'disk_path, alert_operator, threshold_num, threshold_len, pattern, ' ||
    'channels, message_template, cooldown_sec, ' ||
    'next_run_at, locked_until, locked_by, last_run_at, last_status, ' ||
    'last_fired_at, last_notification_output_length, created_at, updated_at' ||
    ') VALUES (' ||
    id || ', ' ||
    quote_literal(name) || ', ' ||
    server_id || ', ' ||
    quote_literal(monitoring_type) || ', ' ||
    enabled || ', ' ||
    interval_sec || ', ' ||
    COALESCE(ssh_port::text, 'NULL') || ', ' ||
    COALESCE(quote_literal(ssh_username), 'NULL') || ', ' ||
    COALESCE(quote_literal(ssh_password), 'NULL') || ', ' ||
    COALESCE(quote_literal(ssh_private_key_path), 'NULL') || ', ' ||
    COALESCE(quote_literal(db_type), 'NULL') || ', ' ||
    COALESCE(db_port::text, 'NULL') || ', ' ||
    COALESCE(quote_literal(db_name), 'NULL') || ', ' ||
    COALESCE(quote_literal(db_url), 'NULL') || ', ' ||
    COALESCE(quote_literal(db_username), 'NULL') || ', ' ||
    COALESCE(quote_literal(db_password), 'NULL') || ', ' ||
    COALESCE(quote_literal(shell_script), 'NULL') || ', ' ||
    COALESCE(quote_literal(log_file_path), 'NULL') || ', ' ||
    COALESCE(quote_literal(include_keywords), 'NULL') || ', ' ||
    COALESCE(quote_literal(exclude_keywords), 'NULL') || ', ' ||
    COALESCE(quote_literal(disk_path), 'NULL') || ', ' ||
    quote_literal(alert_operator) || ', ' ||
    COALESCE(threshold_num::text, 'NULL') || ', ' ||
    COALESCE(threshold_len::text, 'NULL') || ', ' ||
    COALESCE(quote_literal(pattern), 'NULL') || ', ' ||
    COALESCE(quote_literal(channels), 'NULL') || ', ' ||
    quote_literal(message_template) || ', ' ||
    cooldown_sec || ', ' ||
    quote_literal(next_run_at::text) || '::timestamptz, ' ||
    CASE WHEN locked_until IS NULL THEN 'NULL' ELSE quote_literal(locked_until::text) || '::timestamptz' END || ', ' ||
    COALESCE(quote_literal(locked_by), 'NULL') || ', ' ||
    CASE WHEN last_run_at IS NULL THEN 'NULL' ELSE quote_literal(last_run_at::text) || '::timestamptz' END || ', ' ||
    COALESCE(quote_literal(last_status), 'NULL') || ', ' ||
    CASE WHEN last_fired_at IS NULL THEN 'NULL' ELSE quote_literal(last_fired_at::text) || '::timestamptz' END || ', ' ||
    COALESCE(last_notification_output_length::text, 'NULL') || ', ' ||
    quote_literal(created_at::text) || '::timestamptz, ' ||
    quote_literal(updated_at::text) || '::timestamptz' ||
    ');' AS insert_statement
FROM monitoring_rules
ORDER BY id;

-- 사용 방법:
-- 1. 위 쿼리를 실행하여 INSERT 문을 생성합니다.
-- 2. 결과의 insert_statement 컬럼을 복사합니다.
-- 3. 다른 데이터베이스에서 INSERT 문을 실행합니다.
-- 4. 아래 시퀀스 보정 쿼리를 실행하여 시퀀스를 올바르게 설정합니다.

-- 시퀀스 보정 (INSERT 후 실행)
-- SELECT setval('monitoring_rules_id_seq', (SELECT COALESCE(MAX(id), 1) FROM monitoring_rules));
