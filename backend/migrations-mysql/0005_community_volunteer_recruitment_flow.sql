ALTER TABLE community_volunteer_posts
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'open' AFTER image_url,
  ADD COLUMN deadline_at VARCHAR(32) NULL AFTER status,
  ADD KEY idx_community_volunteer_posts_status (status, deadline_at, created_at DESC),
  ADD CONSTRAINT chk_community_volunteer_posts_status CHECK (status IN ('open', 'closed'));

ALTER TABLE community_volunteer_applications
  DROP CHECK chk_community_volunteer_applications_status,
  ADD CONSTRAINT chk_community_volunteer_applications_status
    CHECK (status IN ('pending', 'approved', 'rejected', 'cancelled'));

INSERT IGNORE INTO community_volunteer_posts (
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

INSERT IGNORE INTO community_volunteer_posts (
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

INSERT IGNORE INTO community_volunteer_posts (
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
