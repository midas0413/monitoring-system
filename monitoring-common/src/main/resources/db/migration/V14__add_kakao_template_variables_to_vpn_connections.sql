-- V14: VPN 연결에 카카오 템플릿 변수 컬럼 추가

ALTER TABLE vpn_connections
ADD COLUMN IF NOT EXISTS kakao_template_variables TEXT NULL;

COMMENT ON COLUMN vpn_connections.kakao_template_variables IS '카카오 템플릿 변수 값 (JSON 형식: {"변수명": "값"}). 템플릿 변수 입력 시 저장됨';
