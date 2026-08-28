CREATE TABLE IF NOT EXISTS ai_memory_items (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  companion_key TEXT NOT NULL,
  memory_type TEXT NOT NULL DEFAULT 'fact'
    CHECK (memory_type IN ('profile', 'preference', 'event', 'boundary', 'story', 'fact')),
  memory_key TEXT NOT NULL,
  content TEXT NOT NULL,
  source_message_id TEXT,
  confidence REAL NOT NULL DEFAULT 0.8,
  importance INTEGER NOT NULL DEFAULT 50,
  last_used_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  expires_at TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ai_memory_items_key
  ON ai_memory_items(user_id, companion_key, memory_key);

CREATE INDEX IF NOT EXISTS idx_ai_memory_items_lookup
  ON ai_memory_items(user_id, companion_key, importance DESC, updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_memory_settings (
  user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  enabled INTEGER NOT NULL DEFAULT 0 CHECK (enabled IN (0, 1)),
  consented_at TEXT,
  updated_at TEXT NOT NULL
);
