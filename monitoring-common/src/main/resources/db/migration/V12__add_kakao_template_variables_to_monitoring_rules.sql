-- 모니터링 규칙 테이블에 카카오 템플릿 변수 컬럼 추가
ALTER TABLE monitoring_rules
ADD COLUMN IF NOT EXISTS kakao_template_variables TEXT NULL;

COMMENT ON COLUMN monitoring_rules.kakao_template_variables IS '카카오 템플릿 변수 값 (JSON 형식: {"변수명": "값"}). 템플릿 변수 입력 시 저장됨';
