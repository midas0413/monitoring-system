-- VPN 연결 테이블에 카카오 템플릿 코드 컬럼 추가
ALTER TABLE vpn_connections
ADD COLUMN IF NOT EXISTS kakao_template_code VARCHAR(100) NULL;

COMMENT ON COLUMN vpn_connections.kakao_template_code IS '카카오 알림톡 템플릿 ID (Aligo tpl_code). KAKAO 채널 사용 시 선택';
