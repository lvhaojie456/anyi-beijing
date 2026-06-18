ALTER TABLE users ADD COLUMN wechat_openid TEXT;
ALTER TABLE users ADD COLUMN wechat_unionid TEXT;
ALTER TABLE users ADD COLUMN wechat_nickname TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_wechat_openid
ON users (wechat_openid)
WHERE wechat_openid IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_wechat_unionid
ON users (wechat_unionid)
WHERE wechat_unionid IS NOT NULL;
