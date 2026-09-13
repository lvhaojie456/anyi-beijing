ALTER TABLE ai_chat_messages ADD COLUMN message_type TEXT NOT NULL DEFAULT 'text';
ALTER TABLE ai_chat_messages ADD COLUMN duration_ms INTEGER;
ALTER TABLE ai_chat_messages ADD COLUMN audio_mime_type TEXT;
ALTER TABLE ai_chat_messages ADD COLUMN audio_url TEXT;
ALTER TABLE ai_chat_messages ADD COLUMN audio_asset_id TEXT;
ALTER TABLE ai_chat_messages ADD COLUMN client_request_id TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS idx_ai_chat_messages_request
  ON ai_chat_messages(user_id, companion_id, client_request_id)
  WHERE client_request_id IS NOT NULL;
