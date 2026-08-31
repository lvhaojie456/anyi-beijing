ALTER TABLE users
  ADD COLUMN ai_companion_list_background_url TEXT;

ALTER TABLE ai_companions
  ADD COLUMN chat_background_url TEXT;
