ALTER TABLE users
  ADD COLUMN terms_accepted_at VARCHAR(32) NULL,
  ADD COLUMN privacy_accepted_at VARCHAR(32) NULL;

ALTER TABLE assets
  ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'private',
  ADD KEY idx_assets_visibility (visibility, created_at);

ALTER TABLE community_posts
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'approved',
  ADD COLUMN moderation_reason VARCHAR(300) NULL,
  ADD COLUMN moderated_at VARCHAR(32) NULL,
  ADD COLUMN moderated_by VARCHAR(64) NULL,
  ADD KEY idx_community_posts_status (status, created_at);

ALTER TABLE community_post_comments
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'approved',
  ADD COLUMN moderation_reason VARCHAR(300) NULL,
  ADD COLUMN moderated_at VARCHAR(32) NULL,
  ADD COLUMN moderated_by VARCHAR(64) NULL,
  ADD KEY idx_community_post_comments_status (status, created_at);

CREATE TABLE IF NOT EXISTS community_reports (
  id VARCHAR(64) NOT NULL,
  reporter_id VARCHAR(64) NOT NULL,
  target_type VARCHAR(16) NOT NULL,
  target_id VARCHAR(64) NOT NULL,
  reason VARCHAR(300) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'pending',
  reviewer_id VARCHAR(64) NULL,
  reviewed_at VARCHAR(32) NULL,
  created_at VARCHAR(32) NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY idx_community_reports_unique (reporter_id, target_type, target_id),
  KEY idx_community_reports_status (status, created_at),
  KEY idx_community_reports_target (target_type, target_id, created_at),
  CONSTRAINT fk_community_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_community_reports_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
  CONSTRAINT chk_community_reports_target CHECK (target_type IN ('post', 'comment')),
  CONSTRAINT chk_community_reports_status CHECK (status IN ('pending', 'actioned', 'dismissed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_moderation (
  user_id VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  reason VARCHAR(300) NULL,
  expires_at VARCHAR(32) NULL,
  updated_by VARCHAR(64) NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id),
  KEY idx_user_moderation_status (status, updated_at),
  CONSTRAINT fk_user_moderation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_moderation_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
  CONSTRAINT chk_user_moderation_status CHECK (status IN ('blocked', 'banned'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
