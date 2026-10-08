-- Run once in an isolated database, never against the existing Stella application's data.
CREATE TABLE IF NOT EXISTS retailers (
  id TEXT PRIMARY KEY,
  slug TEXT NOT NULL UNIQUE,
  display_name TEXT NOT NULL,
  brand_color TEXT NOT NULL DEFAULT '#FFDA00',
  logo_url TEXT,
  commission_percent INTEGER NOT NULL DEFAULT 30 CHECK (commission_percent BETWEEN 0 AND 80),
  payfast_receiver_id TEXT,
  split_approved BOOLEAN NOT NULL DEFAULT FALSE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  access_token_hash TEXT NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS orders (
  id TEXT PRIMARY KEY,
  retailer_id TEXT NOT NULL REFERENCES retailers(id),
  buyer_email TEXT NOT NULL,
  token_hash TEXT NOT NULL,
  amount_cents INTEGER NOT NULL DEFAULT 5000 CHECK (amount_cents = 5000),
  commission_percent INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','paid','cancelled','refunded')),
  pf_payment_id TEXT UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  paid_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_orders_retailer ON orders(retailer_id,created_at);
CREATE TABLE IF NOT EXISTS licenses (
  id TEXT PRIMARY KEY,
  order_id TEXT NOT NULL UNIQUE REFERENCES orders(id),
  serial_hash TEXT NOT NULL UNIQUE,
  serial_cipher TEXT NOT NULL,
  bound_device_hash TEXT,
  installation_id TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  activated_at TIMESTAMPTZ
);
CREATE TABLE IF NOT EXISTS activation_events (
  id TEXT PRIMARY KEY,
  license_id TEXT NOT NULL REFERENCES licenses(id),
  event_type TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
