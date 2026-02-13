-- Flyway Schema History 테이블 업데이트 스크립트
-- 모든 마이그레이션을 V1으로 통합한 후 실행

-- 기존 마이그레이션 기록 삭제 (V2, V5, V6, V9, V10)
DELETE FROM flyway_schema_history 
WHERE version IN ('2', '5', '6', '9', '10');

-- V1 마이그레이션 기록이 없으면 추가
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT 
    1,
    '1',
    'init',
    'SQL',
    'V1__init.sql',
    NULL,
    current_user,
    now(),
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '1'
);

-- V1 마이그레이션 기록 업데이트 (이미 존재하는 경우)
UPDATE flyway_schema_history
SET 
    installed_rank = 1,
    description = 'init',
    type = 'SQL',
    script = 'V1__init.sql',
    installed_on = now(),
    execution_time = 0,
    success = true
WHERE version = '1';
