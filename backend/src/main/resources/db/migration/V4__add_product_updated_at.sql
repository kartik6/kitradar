ALTER TABLE product ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE product
SET updated_at = COALESCE(last_seen_at, first_seen_at);

ALTER TABLE product ALTER COLUMN updated_at SET NOT NULL;

CREATE INDEX idx_product_updated_at ON product (updated_at DESC);
