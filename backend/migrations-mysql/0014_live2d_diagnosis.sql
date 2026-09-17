ALTER TABLE live2d_jobs
  ADD COLUMN diagnosis_code VARCHAR(40) NULL AFTER error_code,
  ADD COLUMN suggestion VARCHAR(40) NULL AFTER diagnosis_code,
  ADD COLUMN retry_hint VARCHAR(40) NULL AFTER suggestion,
  ADD COLUMN supervisor_summary TEXT NULL AFTER retry_hint;
