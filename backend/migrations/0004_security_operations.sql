CREATE TABLE IF NOT EXISTS rate_limits (
  bucket_key TEXT NOT NULL,
  route_key TEXT NOT NULL,
  window_start INTEGER NOT NULL,
  count INTEGER NOT NULL,
  updated_at TEXT NOT NULL,
  PRIMARY KEY (bucket_key, route_key, window_start)
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id TEXT PRIMARY KEY,
  actor_id TEXT,
  actor_role TEXT,
  action TEXT NOT NULL,
  target_type TEXT NOT NULL,
  target_id TEXT,
  ip TEXT,
  user_agent TEXT,
  metadata_json TEXT NOT NULL DEFAULT '{}',
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS upload_reviews (
  id TEXT PRIMARY KEY,
  asset_id TEXT NOT NULL,
  owner_id TEXT NOT NULL,
  asset_key TEXT NOT NULL,
  mime_type TEXT NOT NULL,
  size_bytes INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected', 'quarantined')),
  reason TEXT,
  created_at TEXT NOT NULL,
  reviewed_at TEXT,
  reviewed_by TEXT
);

CREATE TABLE IF NOT EXISTS asset_delete_queue (
  id TEXT PRIMARY KEY,
  owner_id TEXT,
  asset_key TEXT NOT NULL,
  reason TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'deleted', 'failed')),
  created_at TEXT NOT NULL,
  processed_at TEXT,
  error_message TEXT
);

CREATE TABLE IF NOT EXISTS crash_reports (
  id TEXT PRIMARY KEY,
  user_id TEXT,
  platform TEXT NOT NULL,
  app_version TEXT,
  device_model TEXT,
  os_version TEXT,
  error_type TEXT NOT NULL,
  message TEXT,
  stack_trace TEXT,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rate_limits_updated ON rate_limits(updated_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_upload_reviews_status ON upload_reviews(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_asset_delete_queue_status ON asset_delete_queue(status, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_crash_reports_created ON crash_reports(created_at DESC);
