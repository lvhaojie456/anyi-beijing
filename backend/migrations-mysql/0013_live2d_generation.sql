ALTER TABLE ai_companions ADD COLUMN live2d_job_id VARCHAR(64) NULL;

CREATE TABLE live2d_jobs (
  id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  companion_id VARCHAR(64) NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  prompt TEXT NOT NULL,
  source_key VARCHAR(512) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'queued',
  stage VARCHAR(40) NOT NULL DEFAULT 'queued',
  progress INT NOT NULL DEFAULT 0,
  error_code VARCHAR(80) NULL,
  active_key VARCHAR(64) NULL UNIQUE,
  lease_token VARCHAR(64) NULL,
  lease_until VARCHAR(32) NULL,
  attempts INT NOT NULL DEFAULT 0,
  created_at VARCHAR(32) NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  UNIQUE KEY live2d_request (user_id, request_id),
  KEY live2d_jobs_owner (user_id, companion_id, created_at),
  KEY live2d_jobs_queue (status, lease_until, created_at),
  CONSTRAINT fk_live2d_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_live2d_companion FOREIGN KEY (companion_id) REFERENCES ai_companions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE live2d_artifacts (
  job_id VARCHAR(64) NOT NULL,
  lease_token VARCHAR(64) NOT NULL,
  name VARCHAR(220) NOT NULL,
  asset_key VARCHAR(512) NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  size_bytes BIGINT NOT NULL,
  mime_type VARCHAR(80) NOT NULL,
  PRIMARY KEY(job_id, lease_token, name),
  CONSTRAINT fk_live2d_artifact_job FOREIGN KEY (job_id) REFERENCES live2d_jobs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
