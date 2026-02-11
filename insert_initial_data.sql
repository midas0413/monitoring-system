-- 초기 데이터 삽입 및 시퀀스 보정 스크립트
-- 쿼리 툴에서 직접 실행 가능

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
('Asia/Dushanbe', '타지키스탄 표준시 (TJT)', 5, 0, '타지키스탄', 11),

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
('Europe/Skopje', '북마케도니아 표준시 (CET/CEST)', 1, 0, '북마케도니아', 30),

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

-- 시스템 코드 초기 데이터 삽입
insert into system_codes (code_type, code_value, code_label, display_order, description) values
-- 서버 용도
('SERVER_PURPOSE', 'WEB', 'WEB 서버', 1, '웹 서버'),
('SERVER_PURPOSE', 'WAS', 'WAS 서버', 2, '웹 애플리케이션 서버'),
('SERVER_PURPOSE', 'DBMS', 'DBMS 서버', 3, '데이터베이스 서버'),
('SERVER_PURPOSE', 'APP', '애플리케이션 서버', 4, '일반 애플리케이션 서버'),
('SERVER_PURPOSE', 'ETC', '기타', 99, '기타 서버'),

-- 모니터링 타입
('MONITORING_TYPE', 'SHELL', 'Shell 스크립트', 1, 'Shell 스크립트 실행 모니터링'),
('MONITORING_TYPE', 'DB', '데이터베이스', 2, '데이터베이스 쿼리 모니터링'),
('MONITORING_TYPE', 'LOGS', '로그 파일', 3, '로그 파일 모니터링'),
('MONITORING_TYPE', 'DISK_SPACE', '디스크 공간', 4, '디스크 공간 모니터링'),

-- 알림 연산자
('ALERT_OPERATOR', 'RUN_FAILED', '실행 실패', 1, '모니터링 실행이 실패한 경우'),
('ALERT_OPERATOR', 'OUTPUT_NUM_GT', '출력값 > 임계값', 2, '출력값이 임계값보다 큰 경우'),
('ALERT_OPERATOR', 'OUTPUT_NUM_LT', '출력값 < 임계값', 3, '출력값이 임계값보다 작은 경우'),
('ALERT_OPERATOR', 'OUTPUT_LEN_GT', '출력 길이 > 임계값', 4, '출력 길이가 임계값보다 큰 경우'),
('ALERT_OPERATOR', 'OUTPUT_LEN_LT', '출력 길이 < 임계값', 5, '출력 길이가 임계값보다 작은 경우'),
('ALERT_OPERATOR', 'OUTPUT_CONTAINS', '출력값 포함', 6, '출력값에 특정 문자열이 포함된 경우'),
('ALERT_OPERATOR', 'OUTPUT_NOT_CONTAINS', '출력값 미포함', 7, '출력값에 특정 문자열이 포함되지 않은 경우'),
('ALERT_OPERATOR', 'OUTPUT_MATCHES', '출력값 정규식 매칭', 8, '출력값이 정규식과 매칭되는 경우');

-- 시퀀스 보정
SELECT setval('servers_id_seq', (SELECT COALESCE(MAX(id), 1) FROM servers));
SELECT setval('vpn_connections_id_seq', (SELECT COALESCE(MAX(id), 1) FROM vpn_connections));
SELECT setval('server_vpn_links_id_seq', (SELECT COALESCE(MAX(id), 1) FROM server_vpn_links));
SELECT setval('monitoring_rules_id_seq', (SELECT COALESCE(MAX(id), 1) FROM monitoring_rules));
SELECT setval('check_runs_id_seq', (SELECT COALESCE(MAX(id), 1) FROM check_runs));
SELECT setval('alert_recipients_id_seq', (SELECT COALESCE(MAX(id), 1) FROM alert_recipients));
SELECT setval('alert_rule_recipient_links_id_seq', (SELECT COALESCE(MAX(id), 1) FROM alert_rule_recipient_links));
SELECT setval('notification_outbox_id_seq', (SELECT COALESCE(MAX(id), 1) FROM notification_outbox));
SELECT setval('notification_settings_id_seq', (SELECT COALESCE(MAX(id), 1) FROM notification_settings));
SELECT setval('timezone_codes_id_seq', (SELECT COALESCE(MAX(id), 1) FROM timezone_codes));
SELECT setval('system_codes_id_seq', (SELECT COALESCE(MAX(id), 1) FROM system_codes));
