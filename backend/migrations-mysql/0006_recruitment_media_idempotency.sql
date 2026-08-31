ALTER TABLE assets
  ADD COLUMN client_request_id VARCHAR(36) NULL;

ALTER TABLE assets
  ADD UNIQUE KEY idx_assets_owner_client_request (owner_id, client_request_id);

ALTER TABLE community_volunteer_posts
  MODIFY COLUMN image_url VARCHAR(1024) NULL;

ALTER TABLE community_volunteer_posts
  ADD COLUMN client_request_id VARCHAR(36) NULL;

ALTER TABLE community_volunteer_posts
  ADD UNIQUE KEY idx_volunteer_posts_admin_client_request (admin_id, client_request_id);
