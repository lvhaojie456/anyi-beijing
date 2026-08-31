ALTER TABLE users
  ADD COLUMN gender VARCHAR(4) NULL AFTER display_name,
  ADD CONSTRAINT chk_users_gender CHECK (gender IN ('男', '女'));
