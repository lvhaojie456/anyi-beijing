CREATE TABLE IF NOT EXISTS ai_memory_items (
  id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  companion_key VARCHAR(64) NOT NULL,
  memory_type VARCHAR(24) NOT NULL DEFAULT 'fact',
  memory_key VARCHAR(128) NOT NULL,
  content TEXT NOT NULL,
  source_message_id VARCHAR(64) NULL,
  confidence DECIMAL(5,4) NOT NULL DEFAULT 0.8000,
  importance INT NOT NULL DEFAULT 50,
  last_used_at VARCHAR(32) NULL,
  created_at VARCHAR(32) NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  expires_at VARCHAR(32) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY idx_ai_memory_items_key (user_id, companion_key, memory_key),
  KEY idx_ai_memory_items_lookup (user_id, companion_key, importance DESC, updated_at DESC),
  CONSTRAINT fk_ai_memory_items_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_ai_memory_items_type CHECK (memory_type IN ('profile', 'preference', 'event', 'boundary', 'story', 'fact'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_memory_settings (
  user_id VARCHAR(64) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  consented_at VARCHAR(32) NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_ai_memory_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_ai_memory_settings_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
