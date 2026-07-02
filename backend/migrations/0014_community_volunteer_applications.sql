CREATE TABLE IF NOT EXISTS community_volunteer_applications (
  id TEXT PRIMARY KEY,
  volunteer_post_id TEXT NOT NULL,
  volunteer_title TEXT NOT NULL,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  phone TEXT NOT NULL,
  note TEXT,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
  reviewer_id TEXT REFERENCES users(id),
  reviewed_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  UNIQUE(volunteer_post_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_community_volunteer_applications_status
  ON community_volunteer_applications(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_community_volunteer_applications_user
  ON community_volunteer_applications(user_id, created_at DESC);
