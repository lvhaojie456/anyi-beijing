ALTER TABLE assets ADD COLUMN client_request_id TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_assets_owner_client_request
  ON assets(owner_id, client_request_id)
  WHERE client_request_id IS NOT NULL;

ALTER TABLE community_volunteer_posts ADD COLUMN client_request_id TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_volunteer_posts_admin_client_request
  ON community_volunteer_posts(admin_id, client_request_id)
  WHERE client_request_id IS NOT NULL;
