ALTER TABLE users ADD COLUMN terms_accepted_at TEXT;
ALTER TABLE users ADD COLUMN privacy_accepted_at TEXT;

ALTER TABLE assets ADD COLUMN visibility TEXT NOT NULL DEFAULT 'private';
CREATE INDEX IF NOT EXISTS idx_assets_visibility ON assets(visibility, created_at DESC);

ALTER TABLE community_posts ADD COLUMN status TEXT NOT NULL DEFAULT 'approved';
ALTER TABLE community_posts ADD COLUMN moderation_reason TEXT;
ALTER TABLE community_posts ADD COLUMN moderated_at TEXT;
ALTER TABLE community_posts ADD COLUMN moderated_by TEXT;
CREATE INDEX IF NOT EXISTS idx_community_posts_status ON community_posts(status, created_at DESC);

ALTER TABLE community_post_comments ADD COLUMN status TEXT NOT NULL DEFAULT 'approved';
ALTER TABLE community_post_comments ADD COLUMN moderation_reason TEXT;
ALTER TABLE community_post_comments ADD COLUMN moderated_at TEXT;
ALTER TABLE community_post_comments ADD COLUMN moderated_by TEXT;
CREATE INDEX IF NOT EXISTS idx_community_post_comments_status
  ON community_post_comments(status, created_at DESC);

CREATE TABLE IF NOT EXISTS community_reports (
  id TEXT PRIMARY KEY,
  reporter_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  target_type TEXT NOT NULL CHECK (target_type IN ('post', 'comment')),
  target_id TEXT NOT NULL,
  reason TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'actioned', 'dismissed')),
  reviewer_id TEXT REFERENCES users(id),
  reviewed_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  UNIQUE(reporter_id, target_type, target_id)
);

CREATE INDEX IF NOT EXISTS idx_community_reports_status
  ON community_reports(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_community_reports_target
  ON community_reports(target_type, target_id, created_at DESC);

CREATE TABLE IF NOT EXISTS user_moderation (
  user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  status TEXT NOT NULL CHECK (status IN ('blocked', 'banned')),
  reason TEXT,
  expires_at TEXT,
  updated_by TEXT REFERENCES users(id),
  updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_moderation_status
  ON user_moderation(status, updated_at DESC);
