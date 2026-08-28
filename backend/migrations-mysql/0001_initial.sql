SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS _node_migrations (
  name VARCHAR(191) NOT NULL,
  applied_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(64) NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(80) NOT NULL,
  role VARCHAR(16) NOT NULL,
  avatar_url VARCHAR(500) NULL,
  created_at VARCHAR(32) NOT NULL,
  deleted_at VARCHAR(32) NULL,
  wechat_openid VARCHAR(128) NULL,
  wechat_unionid VARCHAR(128) NULL,
  wechat_nickname VARCHAR(80) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY idx_users_username (username),
  UNIQUE KEY idx_users_wechat_openid (wechat_openid),
  UNIQUE KEY idx_users_wechat_unionid (wechat_unionid),
  KEY idx_users_deleted_at (deleted_at),
  CONSTRAINT chk_users_role CHECK (role IN ('user', 'admin'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS memorials (
  id VARCHAR(64) NOT NULL,
  owner_id VARCHAR(64) NOT NULL,
  name VARCHAR(80) NOT NULL,
  image_url VARCHAR(500) NULL,
  flower_until_json VARCHAR(2048) NOT NULL DEFAULT '[]',
  candle_until BIGINT NOT NULL DEFAULT 0,
  candle_until_json VARCHAR(2048) NOT NULL DEFAULT '[]',
  fruit_offerings_json VARCHAR(2048) NOT NULL DEFAULT '[]',
  incense_until BIGINT NOT NULL DEFAULT 0,
  created_at VARCHAR(32) NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_memorials_owner (owner_id, created_at DESC),
  CONSTRAINT fk_memorials_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ritual_orders (
  id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  deceased_name VARCHAR(80) NOT NULL,
  relation VARCHAR(60) NOT NULL,
  service_date VARCHAR(64) NULL,
  address VARCHAR(500) NULL,
  plan_id VARCHAR(80) NOT NULL,
  plan_name VARCHAR(120) NOT NULL,
  amount_cents INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  note TEXT NULL,
  acceptance_image_urls VARCHAR(4096) NOT NULL DEFAULT '[]',
  publisher_name VARCHAR(80) NULL,
  needs_clothes TINYINT(1) NOT NULL DEFAULT 0,
  clothes_count INT NOT NULL DEFAULT 0,
  created_at VARCHAR(32) NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ritual_orders_user (user_id, created_at DESC),
  KEY idx_ritual_orders_status (status, created_at DESC),
  CONSTRAINT fk_ritual_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT chk_ritual_orders_status CHECK (
    status IN (
      'pending_payment',
      'pending_order',
      'accepted',
      'in_progress',
      'pending_acceptance',
      'completed',
      'cancelled',
      'refunded'
    )
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_messages (
  id VARCHAR(64) NOT NULL,
  order_id VARCHAR(64) NOT NULL,
  sender_id VARCHAR(64) NOT NULL,
  sender_role VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_order_messages_order (order_id, created_at ASC),
  KEY idx_order_messages_sender (sender_id, created_at DESC),
  CONSTRAINT fk_order_messages_order FOREIGN KEY (order_id) REFERENCES ritual_orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
  CONSTRAINT chk_order_messages_sender_role CHECK (sender_role IN ('user', 'admin'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS talisman_products (
  id VARCHAR(64) NOT NULL,
  name VARCHAR(120) NOT NULL,
  description TEXT NOT NULL,
  price_cents INT NOT NULL,
  image_url VARCHAR(500) NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_talisman_products_active (active, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS talisman_orders (
  id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  product_id VARCHAR(64) NOT NULL,
  amount_cents INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_talisman_orders_user (user_id, created_at DESC),
  KEY idx_talisman_orders_product (product_id, created_at DESC),
  CONSTRAINT fk_talisman_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_talisman_orders_product FOREIGN KEY (product_id) REFERENCES talisman_products(id),
  CONSTRAINT chk_talisman_orders_status CHECK (
    status IN ('pending_payment', 'paid', 'shipping', 'completed', 'cancelled', 'refunded')
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assets (
  id VARCHAR(64) NOT NULL,
  owner_id VARCHAR(64) NOT NULL,
  asset_key VARCHAR(512) NOT NULL,
  url VARCHAR(1024) NOT NULL,
  mime_type VARCHAR(80) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY idx_assets_asset_key (asset_key),
  KEY idx_assets_owner (owner_id, created_at DESC),
  CONSTRAINT fk_assets_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_events (
  id VARCHAR(64) NOT NULL,
  order_type VARCHAR(16) NOT NULL,
  order_id VARCHAR(64) NOT NULL,
  provider VARCHAR(40) NOT NULL,
  provider_trade_no VARCHAR(128) NULL,
  amount_cents INT NOT NULL,
  status VARCHAR(40) NOT NULL,
  raw_json LONGTEXT NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_payment_events_order (order_type, order_id),
  KEY idx_payment_events_provider_trade (provider, provider_trade_no),
  CONSTRAINT chk_payment_events_order_type CHECK (order_type IN ('ritual', 'talisman'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_profiles (
  user_id VARCHAR(64) NOT NULL,
  gender VARCHAR(40) NOT NULL DEFAULT '女性',
  relation VARCHAR(60) NOT NULL DEFAULT '母亲',
  avatar_url VARCHAR(500) NULL,
  smile_avatar_url VARCHAR(500) NULL,
  avatar_motion_json VARCHAR(4096) NOT NULL DEFAULT '{}',
  paid_unlocked TINYINT(1) NOT NULL DEFAULT 0,
  photo_count INT NOT NULL DEFAULT 0,
  voice_count INT NOT NULL DEFAULT 0,
  moment_count INT NOT NULL DEFAULT 0,
  `generated` TINYINT(1) NOT NULL DEFAULT 0,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_ai_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_companions (
  id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  display_name VARCHAR(80) NOT NULL DEFAULT '母亲',
  gender VARCHAR(40) NOT NULL DEFAULT '女性',
  relation VARCHAR(60) NOT NULL DEFAULT '母亲',
  avatar_url VARCHAR(500) NULL,
  smile_avatar_url VARCHAR(500) NULL,
  avatar_motion_json LONGTEXT NOT NULL,
  paid_unlocked TINYINT(1) NOT NULL DEFAULT 0,
  photo_count INT NOT NULL DEFAULT 0,
  voice_count INT NOT NULL DEFAULT 0,
  moment_count INT NOT NULL DEFAULT 0,
  `generated` TINYINT(1) NOT NULL DEFAULT 0,
  avatar_style_json LONGTEXT NOT NULL,
  kernel_json LONGTEXT NOT NULL,
  is_default TINYINT(1) NOT NULL DEFAULT 0,
  created_at VARCHAR(32) NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ai_companions_user (user_id, updated_at DESC),
  KEY idx_ai_companions_default (user_id, is_default DESC, created_at ASC),
  CONSTRAINT fk_ai_companions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_chat_messages (
  id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  companion_id VARCHAR(64) NULL,
  sender VARCHAR(16) NOT NULL,
  content LONGTEXT NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ai_chat_messages_user (user_id, created_at ASC),
  KEY idx_ai_chat_messages_companion (user_id, companion_id, created_at ASC),
  CONSTRAINT fk_ai_chat_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_ai_chat_messages_sender CHECK (sender IN ('user', 'ai'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS feature_unlocks (
  user_id VARCHAR(64) NOT NULL,
  feature VARCHAR(80) NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, feature),
  CONSTRAINT fk_feature_unlocks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS account_deletion_requests (
  id VARCHAR(64) NOT NULL,
  username VARCHAR(64) NOT NULL,
  contact VARCHAR(120) NULL,
  reason TEXT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  created_at VARCHAR(32) NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_account_deletion_requests_status (status, created_at DESC),
  CONSTRAINT chk_account_deletion_requests_status CHECK (status IN ('pending', 'processing', 'completed', 'rejected'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rate_limits (
  bucket_key VARCHAR(191) NOT NULL,
  route_key VARCHAR(191) NOT NULL,
  window_start BIGINT NOT NULL,
  count INT NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (bucket_key, route_key, window_start),
  KEY idx_rate_limits_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
  id VARCHAR(64) NOT NULL,
  actor_id VARCHAR(64) NULL,
  actor_role VARCHAR(16) NULL,
  action VARCHAR(120) NOT NULL,
  target_type VARCHAR(120) NOT NULL,
  target_id VARCHAR(64) NULL,
  ip VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  metadata_json LONGTEXT NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_audit_logs_actor (actor_id, created_at DESC),
  KEY idx_audit_logs_action (action, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS upload_reviews (
  id VARCHAR(64) NOT NULL,
  asset_id VARCHAR(64) NOT NULL,
  owner_id VARCHAR(64) NOT NULL,
  asset_key VARCHAR(512) NOT NULL,
  mime_type VARCHAR(80) NOT NULL,
  size_bytes BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  reason VARCHAR(300) NULL,
  created_at VARCHAR(32) NOT NULL,
  reviewed_at VARCHAR(32) NULL,
  reviewed_by VARCHAR(64) NULL,
  PRIMARY KEY (id),
  KEY idx_upload_reviews_status (status, created_at DESC),
  KEY idx_upload_reviews_asset_key (asset_key),
  KEY idx_upload_reviews_owner (owner_id, created_at DESC),
  CONSTRAINT chk_upload_reviews_status CHECK (status IN ('pending', 'approved', 'rejected', 'quarantined'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS asset_delete_queue (
  id VARCHAR(64) NOT NULL,
  owner_id VARCHAR(64) NULL,
  asset_key VARCHAR(512) NOT NULL,
  reason VARCHAR(120) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  created_at VARCHAR(32) NOT NULL,
  processed_at VARCHAR(32) NULL,
  error_message TEXT NULL,
  PRIMARY KEY (id),
  KEY idx_asset_delete_queue_status (status, created_at ASC),
  KEY idx_asset_delete_queue_asset_key (asset_key),
  CONSTRAINT chk_asset_delete_queue_status CHECK (status IN ('pending', 'deleted', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS crash_reports (
  id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NULL,
  platform VARCHAR(40) NOT NULL,
  app_version VARCHAR(40) NULL,
  device_model VARCHAR(120) NULL,
  os_version VARCHAR(80) NULL,
  error_type VARCHAR(160) NOT NULL,
  message VARCHAR(1000) NULL,
  stack_trace LONGTEXT NULL,
  created_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_crash_reports_created (created_at DESC),
  KEY idx_crash_reports_user (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS community_posts (
  id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  content TEXT NOT NULL,
  image_urls VARCHAR(4096) NOT NULL DEFAULT '[]',
  created_at VARCHAR(32) NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_community_posts_created (created_at DESC),
  KEY idx_community_posts_user (user_id, created_at DESC),
  CONSTRAINT fk_community_posts_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS community_post_likes (
  post_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (post_id, user_id),
  KEY idx_community_post_likes_user (user_id, created_at DESC),
  CONSTRAINT fk_community_post_likes_post FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE,
  CONSTRAINT fk_community_post_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS community_volunteer_posts (
  id VARCHAR(64) NOT NULL,
  admin_id VARCHAR(64) NOT NULL,
  title VARCHAR(120) NOT NULL,
  body TEXT NOT NULL,
  contact VARCHAR(200) NULL,
  image_url VARCHAR(500) NULL,
  created_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_community_volunteer_posts_created (created_at DESC),
  KEY idx_community_volunteer_posts_admin (admin_id, created_at DESC),
  CONSTRAINT fk_community_volunteer_posts_admin FOREIGN KEY (admin_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS community_volunteer_applications (
  id VARCHAR(64) NOT NULL,
  volunteer_post_id VARCHAR(64) NOT NULL,
  volunteer_title VARCHAR(120) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  name VARCHAR(40) NOT NULL,
  phone VARCHAR(40) NOT NULL,
  note VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  reviewer_id VARCHAR(64) NULL,
  reviewed_at VARCHAR(32) NULL,
  created_at VARCHAR(32) NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY idx_community_volunteer_applications_unique (volunteer_post_id, user_id),
  KEY idx_community_volunteer_applications_status (status, created_at DESC),
  KEY idx_community_volunteer_applications_user (user_id, created_at DESC),
  KEY idx_community_volunteer_applications_reviewer (reviewer_id, reviewed_at DESC),
  CONSTRAINT fk_community_volunteer_applications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_community_volunteer_applications_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
  CONSTRAINT chk_community_volunteer_applications_status CHECK (status IN ('pending', 'approved', 'rejected'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS community_post_comments (
  id VARCHAR(64) NOT NULL,
  post_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  content TEXT NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  updated_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_community_post_comments_post (post_id, created_at ASC),
  KEY idx_community_post_comments_user (user_id, created_at DESC),
  CONSTRAINT fk_community_post_comments_post FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE,
  CONSTRAINT fk_community_post_comments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO talisman_products (id, name, description, price_cents, image_url, active, created_at)
VALUES
  ('peace-charm', '平安护符', '平安随身护符，适合日常佩戴与祈愿。', 990, NULL, 1, DATE_FORMAT(UTC_TIMESTAMP(), '%Y-%m-%dT%H:%i:%sZ')),
  ('lotus-card', '莲花心愿牌', '用于写下心愿与纪念寄语的莲花心愿牌。', 1990, NULL, 1, DATE_FORMAT(UTC_TIMESTAMP(), '%Y-%m-%dT%H:%i:%sZ')),
  ('long-light-gift', '长明灯礼盒', '长明灯与香薰组合礼盒，适合作为纪念礼。', 4990, NULL, 1, DATE_FORMAT(UTC_TIMESTAMP(), '%Y-%m-%dT%H:%i:%sZ'));
