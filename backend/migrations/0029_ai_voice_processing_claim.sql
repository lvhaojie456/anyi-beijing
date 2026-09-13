ALTER TABLE ai_chat_messages ADD COLUMN voice_status TEXT NOT NULL DEFAULT 'ready';
ALTER TABLE ai_chat_messages ADD COLUMN voice_processing_at TEXT;
