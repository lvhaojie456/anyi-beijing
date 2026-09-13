ALTER TABLE ai_chat_messages
  ADD COLUMN voice_status VARCHAR(16) NOT NULL DEFAULT 'ready',
  ADD COLUMN voice_processing_at VARCHAR(32) NULL;
