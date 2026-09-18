ALTER TABLE ai_companions ADD COLUMN voice_id TEXT;
CREATE TABLE IF NOT EXISTS ai_speech_usage (
  user_id TEXT NOT NULL,
  day TEXT NOT NULL,
  characters INTEGER NOT NULL DEFAULT 0,
  updated_at TEXT NOT NULL,
  PRIMARY KEY (user_id, day)
);
