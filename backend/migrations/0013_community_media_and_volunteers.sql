ALTER TABLE community_posts ADD COLUMN image_urls TEXT NOT NULL DEFAULT '[]';

CREATE TABLE IF NOT EXISTS community_volunteer_posts (
  id TEXT PRIMARY KEY,
  admin_id TEXT NOT NULL REFERENCES users(id),
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  contact TEXT,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_community_volunteer_posts_created
  ON community_volunteer_posts(created_at DESC);
