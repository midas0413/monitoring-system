-- V13: 카카오 템플릿에 템플릿 메시지 형태와 버튼 정보 컬럼 추가

ALTER TABLE kakao_templates
ADD COLUMN IF NOT EXISTS template_message TEXT NULL;

ALTER TABLE kakao_templates
ADD COLUMN IF NOT EXISTS button_info TEXT NULL;

COMMENT ON COLUMN kakao_templates.template_message IS '템플릿 메시지 형태 (알리고에 등록된 템플릿 본문 형태, #{변수명} 형식 사용)';
COMMENT ON COLUMN kakao_templates.button_info IS '버튼 정보 (JSON 형식: {"button":[{"name":"버튼명","linkType":"AC","linkTypeName":"채널 추가"}]})';
