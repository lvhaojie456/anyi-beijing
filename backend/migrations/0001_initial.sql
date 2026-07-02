PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  username TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  display_name TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('user', 'admin')),
  avatar_url TEXT,
  created_at TEXT NOT NULL,
  deleted_at TEXT
);

CREATE TABLE IF NOT EXISTS memorials (
  id TEXT PRIMARY KEY,
  owner_id TEXT NOT NULL REFERENCES users(id),
  name TEXT NOT NULL,
  image_url TEXT,
  flower_until_json TEXT NOT NULL DEFAULT '[]',
  candle_until INTEGER NOT NULL DEFAULT 0,
  candle_until_json TEXT NOT NULL DEFAULT '[]',
  fruit_offerings_json TEXT NOT NULL DEFAULT '[]',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS ritual_orders (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  deceased_name TEXT NOT NULL,
  relation TEXT NOT NULL,
  service_date TEXT,
  address TEXT,
  plan_id TEXT NOT NULL,
  plan_name TEXT NOT NULL,
  amount_cents INTEGER NOT NULL,
  status TEXT NOT NULL CHECK (
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
  ),
  note TEXT,
  acceptance_image_urls TEXT NOT NULL DEFAULT '[]',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS order_messages (
  id TEXT PRIMARY KEY,
  order_id TEXT NOT NULL REFERENCES ritual_orders(id) ON DELETE CASCADE,
  sender_id TEXT NOT NULL REFERENCES users(id),
  sender_role TEXT NOT NULL CHECK (sender_role IN ('user', 'admin')),
  content TEXT NOT NULL,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS talisman_products (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT NOT NULL,
  price_cents INTEGER NOT NULL,
  image_url TEXT,
  active INTEGER NOT NULL DEFAULT 1,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS talisman_orders (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  product_id TEXT NOT NULL REFERENCES talisman_products(id),
  amount_cents INTEGER NOT NULL,
  status TEXT NOT NULL CHECK (
    status IN ('pending_payment', 'paid', 'shipping', 'completed', 'cancelled', 'refunded')
  ),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS assets (
  id TEXT PRIMARY KEY,
  owner_id TEXT NOT NULL REFERENCES users(id),
  asset_key TEXT NOT NULL UNIQUE,
  url TEXT NOT NULL,
  mime_type TEXT NOT NULL,
  size_bytes INTEGER NOT NULL,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS payment_events (
  id TEXT PRIMARY KEY,
  order_type TEXT NOT NULL CHECK (order_type IN ('ritual', 'talisman')),
  order_id TEXT NOT NULL,
  provider TEXT NOT NULL,
  provider_trade_no TEXT,
  amount_cents INTEGER NOT NULL,
  status TEXT NOT NULL,
  raw_json TEXT NOT NULL,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ritual_orders_user ON ritual_orders(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ritual_orders_status ON ritual_orders(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_messages_order ON order_messages(order_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_talisman_orders_user ON talisman_orders(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_events_order ON payment_events(order_type, order_id);

INSERT OR IGNORE INTO talisman_products (id, name, description, price_cents, image_url, active, created_at)
VALUES
  ('peace-charm', '平安护符', '平安随身护符，适合日常佩戴与祈愿。', 990, NULL, 1, datetime('now')),
  ('lotus-card', '莲花心愿牌', '用于写下心愿与纪念寄语的莲花心愿牌。', 1990, NULL, 1, datetime('now')),
  ('long-light-gift', '长明灯礼盒', '长明灯与香薰组合礼盒，适合作为纪念礼。', 4990, NULL, 1, datetime('now'));
