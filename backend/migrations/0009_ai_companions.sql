CREATE TABLE IF NOT EXISTS ai_companions (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  display_name TEXT NOT NULL DEFAULT '母亲',
  gender TEXT NOT NULL DEFAULT '女性',
  relation TEXT NOT NULL DEFAULT '母亲',
  avatar_url TEXT,
  paid_unlocked INTEGER NOT NULL DEFAULT 0,
  photo_count INTEGER NOT NULL DEFAULT 0,
  voice_count INTEGER NOT NULL DEFAULT 0,
  moment_count INTEGER NOT NULL DEFAULT 0,
  generated INTEGER NOT NULL DEFAULT 0,
  is_default INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_companions_user ON ai_companions(user_id, updated_at DESC);

INSERT INTO ai_companions (
  id, user_id, display_name, gender, relation, avatar_url, paid_unlocked,
  photo_count, voice_count, moment_count, generated, is_default, created_at, updated_at
)
SELECT
  lower(hex(randomblob(16))),
  user_id,
  relation,
  gender,
  relation,
  avatar_url,
  paid_unlocked,
  photo_count,
  voice_count,
  moment_count,
  generated,
  1,
  updated_at,
  updated_at
FROM ai_profiles
WHERE NOT EXISTS (
  SELECT 1 FROM ai_companions WHERE ai_companions.user_id = ai_profiles.user_id
);

ALTER TABLE ai_chat_messages ADD COLUMN companion_id TEXT;

UPDATE ai_chat_messages
SET companion_id = (
  SELECT id
  FROM ai_companions
  WHERE ai_companions.user_id = ai_chat_messages.user_id
  ORDER BY is_default DESC, created_at ASC
  LIMIT 1
)
WHERE companion_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_companion ON ai_chat_messages(user_id, companion_id, created_at ASC);
