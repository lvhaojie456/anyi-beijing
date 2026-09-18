ALTER TABLE ai_companions
  ADD COLUMN voice_id VARCHAR(32) NULL AFTER live2d_model;
CREATE TABLE IF NOT EXISTS ai_speech_usage (
  user_id VARCHAR(64) NOT NULL,
  day VARCHAR(10) NOT NULL,
  characters BIGINT NOT NULL DEFAULT 0,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
