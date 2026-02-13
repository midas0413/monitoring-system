-- 사용자 테이블 생성
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NULL,
    position VARCHAR(50) NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users(enabled);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

COMMENT ON TABLE users IS '시스템 사용자 정보';
COMMENT ON COLUMN users.username IS '로그인 아이디';
COMMENT ON COLUMN users.password IS 'BCrypt 해시된 패스워드';
COMMENT ON COLUMN users.name IS '성명';
COMMENT ON COLUMN users.department IS '부서';
COMMENT ON COLUMN users.position IS '직급';
COMMENT ON COLUMN users.role IS '역할: ADMIN(관리자), USER(일반사용자)';

-- 기본 관리자 계정은 AdminUserInitializer에서 자동 생성됩니다
-- (비밀번호: admin123)
