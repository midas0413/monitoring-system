-- 서버 타임존 정보 (ex: Asia/Seoul, America/New_York)
ALTER TABLE checks ADD COLUMN IF NOT EXISTS timezone varchar(50) NULL;
