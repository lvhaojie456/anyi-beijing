ALTER TABLE community_volunteer_posts
  ADD COLUMN status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'closed'));

ALTER TABLE community_volunteer_posts
  ADD COLUMN deadline_at TEXT;

INSERT OR IGNORE INTO community_volunteer_posts (
  id, admin_id, title, body, contact, image_url, status, deadline_at, created_at
)
SELECT
  'default-story', id, '故事整理义工',
  '协助家属整理纪念故事、照片说明和人生片段，让重要记忆被温柔地保存下来。',
  '在人文社区留言“故事义工”，安忆团队会联系你。',
  NULL, 'open', NULL, '2026-06-05T00:00:00.000Z'
FROM users
WHERE role = 'admin'
ORDER BY created_at ASC
LIMIT 1;

INSERT OR IGNORE INTO community_volunteer_posts (
  id, admin_id, title, body, contact, image_url, status, deadline_at, created_at
)
SELECT
  'default-companion', id, '陪伴倾听义工',
  '为需要倾诉的人提供耐心、克制、尊重边界的陪伴，帮他们把想念慢慢说出来。',
  '在人文社区留言“陪伴义工”报名。',
  NULL, 'open', NULL, '2026-06-05T00:00:00.000Z'
FROM users
WHERE role = 'admin'
ORDER BY created_at ASC
LIMIT 1;

INSERT OR IGNORE INTO community_volunteer_posts (
  id, admin_id, title, body, contact, image_url, status, deadline_at, created_at
)
SELECT
  'default-offline', id, '线下互助义工',
  '参与纪念活动协助、物资整理和线下互助，让社区里的善意真正落到日常里。',
  '在人文社区留言“线下义工”报名。',
  NULL, 'open', NULL, '2026-06-05T00:00:00.000Z'
FROM users
WHERE role = 'admin'
ORDER BY created_at ASC
LIMIT 1;

ALTER TABLE community_volunteer_applications
  RENAME TO community_volunteer_applications_legacy;

CREATE TABLE community_volunteer_applications (
  id TEXT PRIMARY KEY,
  volunteer_post_id TEXT NOT NULL,
  volunteer_title TEXT NOT NULL,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  phone TEXT NOT NULL,
  note TEXT,
  status TEXT NOT NULL DEFAULT 'pending'
    CHECK (status IN ('pending', 'approved', 'rejected', 'cancelled')),
  reviewer_id TEXT REFERENCES users(id),
  reviewed_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  UNIQUE(volunteer_post_id, user_id)
);

INSERT INTO community_volunteer_applications (
  id, volunteer_post_id, volunteer_title, user_id, name, phone, note,
  status, reviewer_id, reviewed_at, created_at, updated_at
)
SELECT
  id, volunteer_post_id, volunteer_title, user_id, name, phone, note,
  status, reviewer_id, reviewed_at, created_at, updated_at
FROM community_volunteer_applications_legacy;

DROP TABLE community_volunteer_applications_legacy;

CREATE INDEX idx_community_volunteer_applications_status
  ON community_volunteer_applications(status, created_at DESC);

CREATE INDEX idx_community_volunteer_applications_user
  ON community_volunteer_applications(user_id, created_at DESC);

CREATE INDEX idx_community_volunteer_posts_status
  ON community_volunteer_posts(status, deadline_at, created_at DESC);
