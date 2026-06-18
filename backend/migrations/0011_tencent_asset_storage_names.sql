ALTER TABLE assets RENAME COLUMN r2_key TO asset_key;
ALTER TABLE upload_reviews RENAME COLUMN r2_key TO asset_key;
ALTER TABLE r2_delete_queue RENAME TO asset_delete_queue;
DROP INDEX IF EXISTS idx_r2_delete_queue_status;
CREATE INDEX IF NOT EXISTS idx_asset_delete_queue_status ON asset_delete_queue(status, created_at ASC);
