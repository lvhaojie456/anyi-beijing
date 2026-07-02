ALTER TABLE ritual_orders ADD COLUMN publisher_name TEXT;
ALTER TABLE ritual_orders ADD COLUMN needs_clothes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ritual_orders ADD COLUMN clothes_count INTEGER NOT NULL DEFAULT 0;
