ALTER TABLE ai_companions ADD COLUMN live2d_job_id TEXT;

CREATE TABLE live2d_jobs (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  companion_id TEXT NOT NULL REFERENCES ai_companions(id) ON DELETE CASCADE,
  request_id TEXT NOT NULL,
  request_hash TEXT NOT NULL,
  prompt TEXT NOT NULL,
  source_key TEXT,
  status TEXT NOT NULL DEFAULT 'queued',
  stage TEXT NOT NULL DEFAULT 'queued',
  progress INTEGER NOT NULL DEFAULT 0,
  error_code TEXT,
  active_key TEXT UNIQUE,
  lease_token TEXT,
  lease_until TEXT,
  attempts INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  UNIQUE(user_id, request_id)
);
CREATE INDEX live2d_jobs_owner ON live2d_jobs(user_id, companion_id, created_at);
CREATE INDEX live2d_jobs_queue ON live2d_jobs(status, lease_until, created_at);

CREATE TABLE live2d_artifacts (
  job_id TEXT NOT NULL REFERENCES live2d_jobs(id) ON DELETE CASCADE,
  lease_token TEXT NOT NULL,
  name TEXT NOT NULL,
  asset_key TEXT NOT NULL,
  sha256 TEXT NOT NULL,
  size_bytes INTEGER NOT NULL,
  mime_type TEXT NOT NULL,
  PRIMARY KEY(job_id, lease_token, name)
);
