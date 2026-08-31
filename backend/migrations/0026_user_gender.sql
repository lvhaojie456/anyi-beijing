ALTER TABLE users
  ADD COLUMN gender TEXT CHECK (gender IN ('男', '女'));
