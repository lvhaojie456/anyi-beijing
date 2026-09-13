ALTER TABLE ai_chat_messages
  ADD COLUMN message_type VARCHAR(16) NOT NULL DEFAULT 'text',
  ADD COLUMN duration_ms INT NULL,
  ADD COLUMN audio_mime_type VARCHAR(80) NULL,
  ADD COLUMN audio_url VARCHAR(1024) NULL,
  ADD COLUMN audio_asset_id VARCHAR(64) NULL,
  ADD COLUMN client_request_id VARCHAR(64) NULL,
  ADD UNIQUE KEY idx_ai_chat_messages_request (user_id, companion_id, client_request_id);
