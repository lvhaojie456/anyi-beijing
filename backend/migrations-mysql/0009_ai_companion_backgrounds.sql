ALTER TABLE users
  ADD COLUMN ai_companion_list_background_url VARCHAR(1024) NULL AFTER avatar_url;

ALTER TABLE ai_companions
  ADD COLUMN chat_background_url VARCHAR(1024) NULL AFTER relation;
